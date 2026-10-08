package com.cambio.converter.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.cambio.converter.ui.theme.AmbientClock
import com.cambio.converter.ui.theme.Palette
import com.cambio.converter.ui.theme.morphBrush
import com.cambio.converter.ui.theme.rememberLoopPhase
import com.cambio.converter.ui.theme.shimmerBrush
import com.composables.icons.lucide.ArrowDownUp
import com.composables.icons.lucide.CircleDollarSign
import com.composables.icons.lucide.Globe
import com.composables.icons.lucide.LoaderCircle
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.RefreshCw
import com.composables.icons.lucide.Wifi
import com.composables.icons.lucide.WifiOff
import kotlin.math.cos
import kotlin.math.sin

private const val TAU = 6.2831855f

/** Atalhos de valor exibidos em "VALORES RÁPIDOS". */
private val QuickValues = listOf(
    1L, 5L, 10L, 50L, 100L, 500L, 1000L,
    2000L, 2500L, 3000L, 3500L, 4000L, 4500L, 5000L, 5500L, 6000L
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConverterScreen(viewModel: ConverterViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Único relógio das animações de fundo (~30 Hz). Ver [AmbientClock].
    LaunchedEffect(Unit) { AmbientClock.run() }
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
                RateLine(text = state.rateText, animate = state.hasRate)
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
            MorphText(
                text = "Câmbio",
                style = TextStyle(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.8).sp,
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
    // O giro e o pulso só existem enquanto carrega. Parado, nenhuma animação
    // roda aqui — antes elas ficavam girando 24h por dia sem fazer sentido.
    val pulse: State<Float>?
    val spin: State<Float>?
    if (isLoading) {
        val infinite = rememberInfiniteTransition(label = "status")
        pulse = infinite.animateFloat(
            initialValue = 0.45f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(750), RepeatMode.Reverse),
            label = "pulse"
        )
        spin = infinite.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(950, easing = LinearEasing)),
            label = "spin"
        )
    } else {
        pulse = null
        spin = null
    }
    val dotColor by animateColorAsState(
        targetValue = when {
            isLoading -> Palette.Violet
            hasNetworkError -> Palette.Amber
            else -> Palette.Mint
        },
        animationSpec = tween(250),
        label = "dot"
    )
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
                    isLoading -> Lucide.LoaderCircle
                    hasNetworkError -> Lucide.WifiOff
                    else -> Lucide.Wifi
                },
                contentDescription = null,
                tint = dotColor,
                modifier = Modifier
                    .size(14.dp)
                    .then(
                        // Lidos dentro da camada: o quadro muda sem recompor a linha.
                        if (pulse != null && spin != null) {
                            Modifier.graphicsLayer {
                                rotationZ = spin.value
                                alpha = pulse.value
                            }
                        } else {
                            Modifier
                        }
                    )
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
                .shimmerBorder(shape = shape)
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
    val pressScale = rememberPressScale(interactionSource, pressedScale = 0.94f)
    val borderColor by animateColorAsState(
        targetValue = if (selected) Palette.Mint else Color.White.copy(alpha = 0.09f),
        animationSpec = tween(240),
        label = "chipBorder"
    )
    Box(
        modifier = modifier
            .height(56.dp)
            .graphicsLayer {
                scaleX = pressScale.value
                scaleY = pressScale.value
            }
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
        MorphText(
            text = symbol,
            colors = listOf(Palette.Mint, Palette.Cyan),
            durationMillis = 5200,
            style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Medium)
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
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "swapRotation"
    )
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(interactionSource, pressedScale = 0.92f)
    // Pulso ambiente no relógio próprio (~30 Hz), não nos 120 Hz do painel.
    val glowPhase = rememberLoopPhase(1900)

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
                // Alpha lido na camada: o brilho respira sem recompor a SwapLayer.
                .graphicsLayer { alpha = 0.55f + 0.45f * glowPhase.value }
                .background(
                    Brush.radialGradient(
                        colors = listOf(Palette.Mint.copy(alpha = 0.30f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(58.dp)
                .graphicsLayer {
                    scaleX = pressScale.value
                    scaleY = pressScale.value
                }
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
                    .rotate(angle)
            )
        }
    }
}

@Composable
private fun ResultValue(text: String) {
    MorphText(
        text = text,
        durationMillis = 7600,
        style = TextStyle(
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1).sp,
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
private fun RateLine(text: String, animate: Boolean) {
    if (animate) {
        MorphText(
            text = text,
            durationMillis = 9000,
            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    } else {
        Text(text = text, style = Palette.BodyMedium, color = Palette.TextSecondary, maxLines = 1)
    }
}

@Composable
private fun SourceCard(book: RateBook?) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Palette.SurfaceBrush, shape)
            .shimmerBorder(shape = shape, peakAlpha = 0.22f, durationMillis = 5200)
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
    val pressScale = rememberPressScale(interactionSource, pressedScale = 0.93f)
    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = pressScale.value
                scaleY = pressScale.value
            }
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
    // Gira só enquanto está atualizando; parado, o ícone fica estático.
    val spin: State<Float>?
    if (loading) {
        val infinite = rememberInfiniteTransition(label = "refresh")
        spin = infinite.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(950, easing = LinearEasing)),
            label = "spin"
        )
    } else {
        spin = null
    }
    val tint by animateColorAsState(
        targetValue = if (loading) Palette.Mint else Palette.TextPrimary,
        animationSpec = tween(200),
        label = "tint"
    )
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(interactionSource, pressedScale = 0.92f)

    Box(
        modifier = Modifier
            .size(50.dp)
            .graphicsLayer {
                scaleX = pressScale.value
                scaleY = pressScale.value
            }
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
            .shimmerBorder(
                shape = CircleShape,
                peakAlpha = if (loading) 0.55f else 0.24f,
                durationMillis = 2600
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
            modifier = Modifier
                .size(23.dp)
                .then(
                    if (spin != null) {
                        Modifier.graphicsLayer { rotationZ = spin.value }
                    } else {
                        Modifier
                    }
                )
        )
    }
}

@Composable
private fun MorphText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    colors: List<Color> = Palette.GradientColors,
    durationMillis: Int = 7000,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val phase = rememberLoopPhase(durationMillis)
    val brush = remember(phase.value, colors) { morphBrush(colors, phase.value) }
    Text(
        text = text,
        modifier = modifier,
        style = style.copy(brush = brush),
        maxLines = maxLines,
        overflow = overflow
    )
}

@Composable
private fun rememberPressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.95f
): State<Float> {
    val pressed by interactionSource.collectIsPressedAsState()
    return animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pressScale"
    )
}

/**
 * Borda com luz que atravessa o vidro. O estado é lido dentro do draw, então a
 * animação acontece sem recompor a tela.
 */
@Composable
private fun Modifier.shimmerBorder(
    shape: Shape,
    strokeWidth: Dp = 1.dp,
    baseAlpha: Float = 0.10f,
    peakAlpha: Float = 0.34f,
    durationMillis: Int = 4200
): Modifier {
    val phase = rememberLoopPhase(durationMillis)
    return drawWithCache {
        val path = Path().also { target ->
            when (val outline = shape.createOutline(size, layoutDirection, this)) {
                is Outline.Rectangle -> target.addRect(outline.rect)
                is Outline.Rounded -> target.addRoundRect(outline.roundRect)
                is Outline.Generic -> target.addPath(outline.path)
            }
        }
        val stroke = strokeWidth.toPx()
        onDrawBehind {
            drawPath(
                path = path,
                color = Color.White.copy(alpha = baseAlpha),
                style = Stroke(width = stroke)
            )
            drawPath(
                path = path,
                brush = shimmerBrush(phase.value, peakAlpha),
                style = Stroke(width = stroke)
            )
        }
    }
}

@Composable
private fun AuroraBackground() {
    // Fases no relógio ambiente (~30 Hz): a aurora anda milímetros por segundo,
    // não precisa dos 120 Hz do painel. Ver [rememberLoopPhase].
    val phaseA = rememberLoopPhase(21_000)
    val phaseB = rememberLoopPhase(27_000)
    val phaseC = rememberLoopPhase(33_000)
    val phaseD = rememberLoopPhase(24_000)

    Box(modifier = Modifier.fillMaxSize()) {
        AuroraGlow(
            size = 360.dp,
            color = Palette.Mint,
            phase = phaseA,
            driftX = 34.dp,
            driftY = 26.dp,
            centerX = (-150).dp,
            centerY = (-90).dp,
            baseAlpha = 0.17f
        )
        AuroraGlow(
            size = 330.dp,
            color = Palette.Violet,
            phase = phaseB,
            driftX = -28.dp,
            driftY = 34.dp,
            centerX = 250.dp,
            centerY = 70.dp,
            baseAlpha = 0.20f
        )
        AuroraGlow(
            size = 430.dp,
            color = Palette.Cyan,
            phase = phaseC,
            driftX = 30.dp,
            driftY = -26.dp,
            centerX = (-90).dp,
            centerY = 600.dp,
            baseAlpha = 0.11f
        )
        AuroraGlow(
            size = 380.dp,
            color = Palette.MintDeep,
            phase = phaseD,
            driftX = -32.dp,
            driftY = -22.dp,
            centerX = 250.dp,
            centerY = 760.dp,
            baseAlpha = 0.15f
        )
    }
}

@Composable
private fun AuroraGlow(
    size: Dp,
    color: Color,
    phase: State<Float>,
    driftX: Dp,
    driftY: Dp,
    centerX: Dp,
    centerY: Dp,
    baseAlpha: Float
) {
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                val t = phase.value * TAU
                // Deslocamento aqui dentro é só uma transformação de camada.
                // Antes usávamos Modifier.offset{}, que rodava o passo de
                // LAYOUT do nó inteiro em todo quadro (o maior custo da aurora).
                translationX = (centerX + driftX * sin(t)).toPx()
                translationY = (centerY + driftY * cos(t)).toPx()
                alpha = baseAlpha * (0.85f + 0.45f * (0.5f + 0.5f * sin(t * 0.7f)))
                val breathe = 1f + 0.10f * sin(t * 0.5f)
                scaleX = breathe
                scaleY = breathe
            }
            .background(
                Brush.radialGradient(
                    colors = listOf(color, Color.Transparent)
                )
            )
    )
}
