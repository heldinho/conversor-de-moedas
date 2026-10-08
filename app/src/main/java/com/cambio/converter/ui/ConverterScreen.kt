package com.cambio.converter.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cambio.converter.ConverterUiState
import com.cambio.converter.ConverterViewModel
import com.cambio.converter.data.Currency
import com.cambio.converter.ui.theme.Palette
import com.composables.icons.lucide.ArrowDownUp
import com.composables.icons.lucide.CircleDollarSign
import com.composables.icons.lucide.Info
import com.composables.icons.lucide.LoaderCircle
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.RefreshCw
import com.composables.icons.lucide.Wifi
import com.composables.icons.lucide.WifiOff

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConverterScreen(viewModel: ConverterViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    var amountBounds by remember { mutableStateOf(Rect.Zero) }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearMessage()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.ScreenBrush)
            .pointerInput(focusManager) {
                awaitEachGesture {
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Initial
                    )
                    val start = down.position
                    val up = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                    if (up != null && !amountBounds.contains(start)) {
                        focusManager.clearFocus()
                    }
                }
            }
    ) {
        AmbientGlow()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 18.dp, bottom = 30.dp)
        ) {
            Header(state = state, onRefresh = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.refresh()
            })

            Spacer(Modifier.height(22.dp))

            SectionLabel("DE")
            GlassCard {
                CurrencyChips(selected = state.from, onSelect = viewModel::setFrom)
                Spacer(Modifier.height(18.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.Hairline))
                Spacer(Modifier.height(14.dp))
                AmountInput(
                    amount = state.input,
                    symbol = state.from.symbol,
                    onAmountChange = viewModel::setInput,
                    modifier = Modifier.onGloballyPositioned { amountBounds = it.boundsInWindow() }
                )
            }

            SwapLayer {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.swap()
            }

            SectionLabel("PARA")
            GlassCard {
                CurrencyChips(selected = state.to, onSelect = viewModel::setTo)
                Spacer(Modifier.height(20.dp))
                Text("RESULTADO", style = LocalTextStyle.current.merge(Palette.LabelSmall), color = Palette.TextSecondary)
                Spacer(Modifier.height(6.dp))
                ResultValue(state = state)
                Spacer(Modifier.height(10.dp))
                RateLine(state = state)
            }

            Spacer(Modifier.height(18.dp))
            SourceCard(state = state)

            Spacer(Modifier.height(20.dp))
            SectionLabel("VALORES RÁPIDOS")
            Spacer(Modifier.height(10.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(1L, 5L, 10L, 50L, 100L, 500L, 1000L).forEach { value ->
                    QuickValueChip(label = value.toString()) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.setQuickValue(value)
                    }
                }
            }

            Spacer(Modifier.height(22.dp))
            Text(
                text = "Cotações atualizadas automaticamente ao abrir o app. " +
                    "Fontes gratuitas: Frankfurter (Banco Central Europeu) e ExchangeRate-API.",
                style = LocalTextStyle.current.copy(fontSize = 11.sp, lineHeight = 16.sp),
                color = Palette.TextSecondary.copy(alpha = 0.8f)
            )
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter)) { data ->
            Snackbar(
                modifier = Modifier.padding(16.dp),
                shape = RoundedCornerShape(18.dp),
                containerColor = Palette.NavyElevated,
                contentColor = Palette.TextPrimary
            ) {
                Text(data.visuals.message, style = LocalTextStyle.current.copy(fontSize = 13.sp))
            }
        }
    }
}

@Composable
private fun Header(state: ConverterUiState, onRefresh: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Lucide.CircleDollarSign,
                    contentDescription = null,
                    tint = Palette.Mint,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(7.dp))
                Text("CONVERSOR DE MOEDAS", style = Palette.LabelSmall, color = Palette.Mint)
            }
            Spacer(Modifier.height(6.dp))
            Text("Câmbio", style = Palette.Headline, color = Palette.TextPrimary)
            Text("Dólar · Euro · Real", style = Palette.BodyMedium, color = Palette.TextSecondary)
        }
        RefreshButton(loading = state.isLoading, onClick = onRefresh)
    }

    Spacer(Modifier.height(14.dp))
    StatusRow(state = state)
}

@Composable
private fun StatusRow(state: ConverterUiState) {
    val infinite = rememberInfiniteTransition(label = "status")
    val pulse by infinite.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(750), RepeatMode.Reverse),
        label = "pulse"
    )
    val spin by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(950, easing = LinearEasing)),
        label = "spin"
    )
    val dotColor by animateColorAsState(
        targetValue = when {
            state.isLoading -> Palette.Violet
            state.hasNetworkError -> Palette.Amber
            else -> Palette.Mint
        },
        animationSpec = tween(250),
        label = "dot"
    )
    val label = when {
        state.isLoading -> "ATUALIZANDO"
        state.hasNetworkError -> "CACHE LOCAL"
        else -> "AO VIVO"
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = when {
                state.isLoading -> Lucide.LoaderCircle
                state.hasNetworkError -> Lucide.WifiOff
                else -> Lucide.Wifi
            },
            contentDescription = null,
            tint = dotColor,
            modifier = Modifier
                .size(15.dp)
                .rotate(if (state.isLoading) spin else 0f)
                .alpha(if (state.isLoading) pulse else 1f)
        )
        Spacer(Modifier.width(7.dp))
        Text(label, style = Palette.LabelSmall, color = dotColor)
        state.book?.let { book ->
            Spacer(Modifier.width(10.dp))
            Text("·", color = Palette.TextSecondary)
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Cotação de ${MoneyFormat.isoDate(book.date)} · ${MoneyFormat.clock(book.fetchedAt)}",
                style = Palette.BodyMedium,
                color = Palette.TextSecondary
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = Palette.LabelSmall,
        color = Palette.TextSecondary,
        modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
    )
}

@Composable
private fun GlassCard(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Palette.CardBrush)
            .border(1.dp, Palette.CardBorder, shape)
            .padding(20.dp),
        content = content
    )
}

@Composable
private fun CurrencyChips(selected: Currency, onSelect: (Currency) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Currency.entries.forEach { currency ->
            CurrencyChip(
                currency = currency,
                selected = currency == selected,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(currency) }
            )
        }
    }
}

@Composable
private fun CurrencyChip(
    currency: Currency,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .height(54.dp)
            .clip(shape)
            .background(
                if (selected) Palette.AccentBrush
                else SolidColor(Color.White.copy(alpha = 0.055f))
            )
            .border(
                width = 1.dp,
                color = if (selected) Palette.Mint else Color.White.copy(alpha = 0.08f),
                shape = shape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${currency.flag}  ${currency.code}",
            color = if (selected) Color(0xFF04231A) else Palette.TextPrimary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = 15.sp
        )
    }
}

@Composable
private fun AmountInput(
    amount: String,
    symbol: String,
    onAmountChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = symbol,
            style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Medium),
            color = Palette.TextSecondary
        )
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = amount,
            onValueChange = onAmountChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(
                color = Palette.TextPrimary,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            ),
            cursorBrush = SolidColor(Palette.Mint),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (amount.isEmpty()) {
                        Text(
                            text = "0,00",
                            style = TextStyle(
                                color = Palette.TextSecondary.copy(alpha = 0.45f),
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
private fun SwapLayer(onSwap: () -> Unit) {
    var rotation by remember { mutableFloatStateOf(0f) }
    val angle by animateFloatAsState(
        targetValue = rotation,
        animationSpec = tween(450, easing = FastOutSlowInEasing),
        label = "swapRotation"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(66.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.14f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(Palette.AccentBrush)
                .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape)
                .clickable {
                    rotation += 180f
                    onSwap()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Lucide.ArrowDownUp,
                contentDescription = "Inverter moedas",
                tint = Color(0xFF04231A),
                modifier = Modifier
                    .size(28.dp)
                    .rotate(angle)
            )
        }
    }
}

@Composable
private fun ResultValue(state: ConverterUiState) {
    val text = if (state.rate == null) "—" else MoneyFormat.money(state.result, state.to)
    Text(
        text = text,
        style = Palette.DisplayValue,
        color = Palette.TextPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun RateLine(state: ConverterUiState) {
    val rate = state.rate
    val text = if (rate == null) {
        "Aguardando cotações…"
    } else {
        "1 ${state.from.code} = ${MoneyFormat.rate(rate)} ${state.to.code}"
    }
    Text(
        text = text,
        style = Palette.BodyMedium,
        color = if (rate == null) Palette.TextSecondary else Palette.Mint,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun SourceCard(state: ConverterUiState) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.07f), shape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Lucide.Info,
            contentDescription = null,
            tint = Palette.Violet,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(12.dp))
        val book = state.book
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = book?.source ?: "Buscando cotações…",
                style = Palette.BodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Palette.TextPrimary
            )
            if (book != null) {
                Text(
                    text = "Referência do dia ${MoneyFormat.isoDate(book.date)}",
                    style = Palette.BodyMedium,
                    color = Palette.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun QuickValueChip(label: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.09f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = Palette.BodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Palette.TextPrimary
        )
    }
}

@Composable
private fun RefreshButton(loading: Boolean, onClick: () -> Unit) {
    val infinite = rememberInfiniteTransition(label = "refresh")
    val spin by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(950, easing = LinearEasing)),
        label = "spin"
    )
    val tint by animateColorAsState(
        targetValue = if (loading) Palette.Mint else Palette.TextPrimary,
        animationSpec = tween(200),
        label = "tint"
    )

    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.dp, Color.White.copy(alpha = 0.13f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Lucide.RefreshCw,
            contentDescription = "Atualizar cotações",
            tint = tint,
            modifier = Modifier
                .size(23.dp)
                .rotate(if (loading) spin else 0f)
        )
    }
}

@Composable
private fun AmbientGlow() {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(340.dp)
                .offset(x = (-130).dp, y = (-70).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Palette.Mint.copy(alpha = 0.13f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(320.dp)
                .offset(x = 230.dp, y = 90.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Palette.Violet.copy(alpha = 0.16f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(420.dp)
                .offset(x = 30.dp, y = 620.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Palette.MintDeep.copy(alpha = 0.10f), Color.Transparent)
                    )
                )
        )
    }
}
