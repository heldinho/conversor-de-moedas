package com.cambio.converter.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

/**
 * Relógio das animações de fundo: ~30 Hz em vez dos 120 Hz do painel.
 *
 * Aurora, brilhos e degradês andam devagar — atualizar 120 vezes por segundo
 * só queimava CPU/GPU (o app gastava ~120% de CPU parado na tela).
 *
 * É UM relógio só para todos os leitores: assim todas as fases mudam dentro do
 * mesmo quadro e a tela é desenhada 30 vezes por segundo, não uma vez por
 * animação (13 animações soltas viravam mais de 60 desenhos por segundo).
 */
private const val AMBIENT_TICK_MS = 33L

object AmbientClock {
    private val _tick = mutableLongStateOf(0L)

    /** Contador compartilhado — ler `.value` agenda o redesenho do nó. */
    val tick: State<Long> get() = _tick

    /** Fase 0..1 (ida e volta) do tempo total desde o início do app. */
    fun phaseOf(tick: Long, durationMillis: Int, reverse: Boolean = true): Float {
        val period = durationMillis.toLong().coerceAtLeast(1L)
        val cycle = if (reverse) period * 2 else period
        val t = ((tick * AMBIENT_TICK_MS) % cycle) / period.toFloat()
        val shaped = if (reverse && t > 1f) 2f - t else t
        return shaped.coerceIn(0f, 1f)
    }

    /** Único loop do app — chamado uma vez, na raiz da tela. */
    suspend fun run() {
        while (coroutineContext.isActive) {
            delay(AMBIENT_TICK_MS)
            _tick.longValue++
        }
    }
}

/**
 * Fase 0..1 que percorre e volta infinitamente em [durationMillis].
 * Ler `.value` no escopo de quem chama garante que o brush seja reconstruído a
 * cada tique do [AmbientClock] (~30 Hz).
 */
@Composable
fun rememberLoopPhase(durationMillis: Int, reverse: Boolean = true): State<Float> {
    val tick = AmbientClock.tick
    return remember(tick, durationMillis, reverse) {
        derivedStateOf { AmbientClock.phaseOf(tick.value, durationMillis, reverse) }
    }
}

/**
 * Cada cor caminha suavemente até a próxima, criando um degradê que "respira".
 * [t] vai de 0 a 1.
 */
fun morphBrush(colors: List<Color>, t: Float): Brush {
    if (colors.isEmpty()) return Brush.linearGradient(listOf(Color.Transparent))
    val fraction = t.coerceIn(0f, 1f)
    val stepped = List(colors.size) { index ->
        lerp(colors[index], colors[(index + 1) % colors.size], fraction)
    }
    return Brush.linearGradient(stepped)
}

/**
 * Faixa de luz que atravessa a borda (brilho tipo vidro polarizado).
 * [phase] vai de 0 a 1.
 */
fun shimmerBrush(phase: Float, peakAlpha: Float = 0.3f): Brush {
    val position = phase.coerceIn(0.001f, 0.999f)
    return Brush.linearGradient(
        0f to Color.Transparent,
        position to Color.White.copy(alpha = peakAlpha),
        1f to Color.Transparent
    )
}
