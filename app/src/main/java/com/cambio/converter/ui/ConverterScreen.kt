package com.cambio.converter.ui

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cambio.converter.ConverterViewModel
import com.cambio.converter.data.Currency
import com.cambio.converter.data.RateBook
import com.cambio.converter.ui.theme.Palette
import com.composables.icons.lucide.ArrowDownUp
import com.composables.icons.lucide.CircleDollarSign
import com.composables.icons.lucide.Globe
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.RefreshCw
import com.composables.icons.lucide.Wifi
import com.composables.icons.lucide.WifiOff

/**
 * Tela sem animações: nada gira, pulsa ou respira.
 *
 * Toda animação ficava na main thread (é o Compose quem agenda os quadros) e
 * competia com o toque e a digitação — era ela que travava o app. Agora a tela
 * só redesenha quando o estado muda (valor digitado, cotação nova, troca de
 * moeda).
 */
private val TitleBrush = Brush.linearGradient(
    colors = listOf(Palette.Cyan, Palette.Mint, Palette.Violet)
)
private val SymbolBrush = Brush.linearGradient(
    colors = listOf(Palette.Mint, Palette.Cyan)
)
private val RateBrush = Brush.horizontalGradient(
    colors = listOf(Palette.Mint, Palette.Cyan, Palette.Mint)
)

/** Atalhos de valor exibidos em "VALORES RÁPIDOS". */
private val QuickValues = listOf(
    1L, 5L, 10L, 50L, 100L, 500L, 1000L,
    2000L, 2500L, 3000L, 3500L, 4000L, 4500L, 5000L, 5500L, 6000L
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConverterScreen(viewModel: ConverterViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    var amountBounds by remember { mutableStateOf(Rect.Zero) }

    // Callbacks memorizados. Sem isso cada tecla criava lambdas novas e nenhuma
    // subtela conseguia pular a recomposição (parâmetro "mudou").
    val onRefresh: () -> Unit = remember(viewModel, haptics) {
        {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.refresh()
        }
    }
    val onSetFrom = remember(viewModel) { viewModel::setFrom }
    val onSetTo = remember(viewModel) { viewModel::setTo }
    val onSwap: () -> Unit = remember(viewModel, haptics) {
        {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.swap()
        }
    }
    val onQuickValue: (Long) -> Unit = remember(viewModel, haptics) { { value ->
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        viewModel.setQuickValue(value)
    } }
    val onAmountChange = remember(viewModel) { viewModel::setInput }

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
        AuroraBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 18.dp, bottom = 30.dp)
        ) {
            Header(
                isLoading = state.isLoading,
                hasNetworkError = state.hasNetworkError,
                book = state.book,
                onRefresh = onRefresh
            )

            Spacer(Modifier.height(22.dp))

            SectionLabel("DE")
            GlassCard {
                CurrencyChips(selected = state.from, onSelect = onSetFrom)
                Spacer(Modifier.height(18.dp))
                GradientDivider()
                Spacer(Modifier.height(14.dp))
                AmountInput(
                    amount = state.input,
                    symbol = state.from.symbol,
                    onAmountChange = onAmountChange,
                    modifier = Modifier.onGloballyPositioned { amountBounds = it.boundsInWindow() }
                )
            }

            SwapLayer(onSwap = onSwap)

            SectionLabel("PARA")
            GlassCard {
                CurrencyChips(selected = state.to, onSelect = onSetTo)
                Spacer(Modifier.height(20.dp))
                Text("RESULTADO", style = Palette.LabelSmall, color = Palette.TextSecondary)
                Spacer(Modifier.height(6.dp))
                ResultValue(text = state.resultText)
                Spacer(Modifier.height(10.dp))
                RateLine(text = state.rateText)
            }

            Spacer(Modifier.height(18.dp))
            SourceCard(book = state.book)

            Spacer(Modifier.height(20.dp))
            SectionLabel("VALORES RÁPIDOS")
            Spacer(Modifier.height(10.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickValues.forEach { value ->
                    QuickValueChip(value = value, onQuick = onQuickValue)
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
private fun Header(
    isLoading: Boolean,
    hasNetworkError: Boolean,
    book: RateBook?,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Palette.AccentBrush),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.CircleDollarSign,
                        contentDescription = null,
                        tint = Palette.OnAccent,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(Modifier.width(9.dp))
                Text(
                    "CONVERSOR DE MOEDAS",
                    style = Palette.LabelSmall.copy(brush = Palette.MintLabelBrush)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Câmbio",
                style = TextStyle(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.8).sp,
                    brush = TitleBrush,
                    shadow = Shadow(
                        color = Palette.Mint.copy(alpha = 0.35f),
                        offset = Offset(0f, 8f),
                        blurRadius = 30f
                    )
                )
            )
            Text("Dólar · Euro · Real", style = Palette.BodyMedium, color = Palette.TextSecondary)
        }
        RefreshButton(loading = isLoading, onClick = onRefresh)
    }

    Spacer(Modifier.height(14.dp))
    StatusRow(isLoading = isLoading, hasNetworkError = hasNetworkError, book = book)
}

@Composable
private fun StatusRow(isLoading: Boolean, hasNetworkError: Boolean, book: RateBook?) {
    val dotColor = when {
        isLoading -> Palette.Violet
        hasNetworkError -> Palette.Amber
        else -> Palette.Mint
    }
    val label = when {
        isLoading -> "ATUALIZANDO"
        hasNetworkError -> "CACHE LOCAL"
        else -> "AO VIVO"
    }
    val pill = RoundedCornerShape(50)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .clip(pill)
                .background(Palette.SurfaceBrush)
                .border(1.dp, Color.White.copy(alpha = 0.09f), pill)
                .padding(horizontal = 13.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when {
                    isLoading -> Lucide.RefreshCw
                    hasNetworkError -> Lucide.WifiOff
                    else -> Lucide.Wifi
                },
                contentDescription = null,
                tint = dotColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(7.dp))
            Text(label, style = Palette.LabelSmall, color = dotColor)
        }
        book?.let {
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Cotação de ${it.dateLabel} · ${it.updatedLabel}",
                style = Palette.BodyMedium,
                color = Palette.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, style = Palette.LabelSmall, color = Palette.TextSecondary)
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Palette.HairlineBrush)
        )
    }
}

@Composable
private fun GradientDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Palette.HairlineBrush)
    )
}

@Composable
private fun GlassCard(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    scaleX = 1.14f
                    scaleY = 1.5f
                }
                .background(
                    Brush.radialGradient(
                        0f to Palette.Mint.copy(alpha = 0.15f),
                        0.5f to Palette.Violet.copy(alpha = 0.13f),
                        1f to Color.Transparent
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Palette.CardBrush, shape)
                .border(1.dp, Palette.StaticBorderBrush, shape)
                .clip(shape)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .background(Palette.TopEdgeBrush)
            )
            Column(
                modifier = Modifier.padding(20.dp),
                content = content
            )
        }
    }
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
    val shape = RoundedCornerShape(18.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val borderColor = if (selected) Palette.Mint else Color.White.copy(alpha = 0.09f)
    Box(
        modifier = modifier
            .height(56.dp)
            .then(
                if (selected) {
                    Modifier.shadow(
                        elevation = 16.dp,
                        shape = shape,
                        ambientColor = Palette.Mint,
                        spotColor = Palette.Cyan
                    )
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(if (selected) Palette.AccentBrush else Palette.SurfaceBrush)
            .border(1.dp, borderColor, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${currency.flag}  ${currency.code}",
            color = if (selected) Palette.OnAccent else Palette.TextPrimary,
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
            style = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                brush = SymbolBrush
            )
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
    // Rotação aplicada direto, sem interpolação: o ícone vira na hora.
    var rotation by remember { mutableFloatStateOf(0f) }
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Palette.HairlineBrush)
        )
        Box(
            modifier = Modifier
                .size(104.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Palette.Mint.copy(alpha = 0.30f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(Palette.AccentBrush)
                .border(1.dp, Color.White.copy(alpha = 0.30f), CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current
                ) {
                    rotation += 180f
                    onSwap()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Lucide.ArrowDownUp,
                contentDescription = "Inverter moedas",
                tint = Palette.OnAccent,
                modifier = Modifier
                    .size(28.dp)
                    .rotate(rotation)
            )
        }
    }
}

@Composable
private fun ResultValue(text: String) {
    Text(
        text = text,
        style = TextStyle(
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1).sp,
            brush = TitleBrush,
            shadow = Shadow(
                color = Palette.Mint.copy(alpha = 0.35f),
                offset = Offset(0f, 10f),
                blurRadius = 34f
            )
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun RateLine(text: String) {
    Text(
        text = text,
        style = Palette.BodyMedium.copy(brush = RateBrush),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun SourceCard(book: RateBook?) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Palette.SurfaceBrush, shape)
            .border(1.dp, Palette.StaticBorderBrush, shape)
            .clip(shape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Lucide.Globe,
            contentDescription = null,
            tint = Palette.Cyan,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = book?.source ?: "Buscando cotações…",
                style = Palette.BodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Palette.TextPrimary
            )
            if (book != null) {
                Text(
                    text = "Referência do dia ${book.dateLabel}",
                    style = Palette.BodyMedium,
                    color = Palette.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun QuickValueChip(value: Long, onQuick: (Long) -> Unit) {
    val label = remember(value) { value.toString() }
    // Lambda memorizado para que o chip pule a recomposição quando nada muda.
    val onClick = remember(value, onQuick) { { onQuick(value) } }
    val shape = RoundedCornerShape(15.dp)
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .clip(shape)
            .background(Palette.SurfaceBrush)
            .border(1.dp, Palette.StaticBorderBrush, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
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
    val tint = if (loading) Palette.Mint else Palette.TextPrimary
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(50.dp)
            .then(
                if (loading) {
                    Modifier.shadow(
                        elevation = 18.dp,
                        shape = CircleShape,
                        ambientColor = Palette.Mint,
                        spotColor = Palette.Cyan
                    )
                } else {
                    Modifier
                }
            )
            .clip(CircleShape)
            .background(Palette.SurfaceBrush)
            .border(
                1.dp,
                if (loading) Palette.StaticBorderBright else Palette.StaticBorderBrush,
                CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Lucide.RefreshCw,
            contentDescription = "Atualizar cotações",
            tint = tint,
            modifier = Modifier.size(23.dp)
        )
    }
}

/** Fundo fixo: manchas de cor paradas, sem deslocamento nem pulso. */
@Composable
private fun AuroraBackground() {
    Box(modifier = Modifier.fillMaxSize()) {
        AuroraGlow(
            size = 360.dp,
            color = Palette.Mint,
            x = (-150).dp,
            y = (-90).dp,
            alpha = 0.17f
        )
        AuroraGlow(
            size = 330.dp,
            color = Palette.Violet,
            x = 250.dp,
            y = 70.dp,
            alpha = 0.20f
        )
        AuroraGlow(
            size = 430.dp,
            color = Palette.Cyan,
            x = (-90).dp,
            y = 600.dp,
            alpha = 0.11f
        )
        AuroraGlow(
            size = 380.dp,
            color = Palette.MintDeep,
            x = 250.dp,
            y = 760.dp,
            alpha = 0.15f
        )
    }
}

@Composable
private fun AuroraGlow(
    size: Dp,
    color: Color,
    x: Dp,
    y: Dp,
    alpha: Float
) {
    Box(
        modifier = Modifier
            .size(size)
            .offset(x = x, y = y)
            .graphicsLayer { this.alpha = alpha }
            .background(
                Brush.radialGradient(
                    colors = listOf(color, Color.Transparent)
                )
            )
    )
}
