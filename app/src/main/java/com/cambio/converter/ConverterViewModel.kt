package com.cambio.converter

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cambio.converter.data.Currency
import com.cambio.converter.data.RateBook
import com.cambio.converter.data.RateRepository
import com.cambio.converter.ui.MoneyFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

data class ConverterUiState(
    val from: Currency = Currency.USD,
    val to: Currency = Currency.BRL,
    val input: String = "1",
    val book: RateBook? = null,
    val isLoading: Boolean = false,
    val hasNetworkError: Boolean = false,
    val message: String? = null,
    /** Textos já formatados — o cálculo acontece em [Dispatchers.Default]. */
    val resultText: String = "—",
    val rateText: String = "Aguardando cotações…",
    val hasRate: Boolean = false
) {
    val rate: Double? get() = book?.rate(from, to)
    val amount: Double get() = MoneyFormat.parse(input) ?: 0.0
    val result: Double get() = rate?.let { amount * it } ?: 0.0
    val isLive: Boolean get() = book != null && !hasNetworkError
}

/**
 * Separação de threads:
 *
 * - **Main (UI)**: só composição, animações e entrada de toque. O Compose exige
 *   que o relógio de animação e o redesenho rodem na thread principal, então as
 *   animações *não* podem ser movidas — mover quebraria o app.
 * - **Dispatchers.Default**: todo o cálculo (parse do valor, multiplicação e
 *   formatação de moeda), longe da thread de UI.
 * - **Dispatchers.IO**: rede e disco (busca e cache das cotações).
 */
class ConverterViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RateRepository(application)

    private val _state = MutableStateFlow(ConverterUiState(book = repository.cached()))
    val state: StateFlow<ConverterUiState> = _state.asStateFlow()

    /**
     * Geração do cálculo: descarta resultados antigos que cheguem fora de ordem
     * (digitação rápida dispara um cálculo por tecla).
     */
    private val computeGeneration = AtomicLong(0L)

    init {
        compute(_state.value)
        refresh()
    }

    private fun compute(draft: ConverterUiState) {
        val generation = computeGeneration.incrementAndGet()
        viewModelScope.launch(Dispatchers.Default) {
            val rate = draft.book?.rate(draft.from, draft.to)
            val amount = MoneyFormat.parse(draft.input) ?: 0.0
            val result = if (rate == null) 0.0 else amount * rate
            val resultText = if (rate == null) "—" else MoneyFormat.money(result, draft.to)
            val rateText = if (rate == null) {
                "Aguardando cotações…"
            } else {
                "1 ${draft.from.code} = ${MoneyFormat.rate(rate)} ${draft.to.code}"
            }
            _state.update { current ->
                if (computeGeneration.get() == generation) {
                    current.copy(resultText = resultText, rateText = rateText, hasRate = rate != null)
                } else {
                    current
                }
            }
        }
    }

    fun refresh() {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, hasNetworkError = false) }

        viewModelScope.launch {
            repository.fetch().fold(
                onSuccess = { book ->
                    _state.update {
                        it.copy(book = book, isLoading = false, hasNetworkError = false, message = null)
                    }
                    compute(_state.value)
                },
                onFailure = {
                    val hasCache = _state.value.book != null
                    _state.update {
                        it.copy(
                            isLoading = false,
                            hasNetworkError = true,
                            message = if (hasCache) {
                                "Sem conexão. Exibindo as últimas cotações salvas."
                            } else {
                                "Não foi possível carregar as cotações. Verifique sua internet."
                            }
                        )
                    }
                    compute(_state.value)
                }
            )
        }
    }

    fun setFrom(currency: Currency) {
        _state.update { current ->
            if (currency == current.to) {
                current.copy(from = currency, to = current.from)
            } else {
                current.copy(from = currency)
            }
        }
        compute(_state.value)
    }

    fun setTo(currency: Currency) {
        _state.update { current ->
            if (currency == current.from) {
                current.copy(from = current.to, to = currency)
            } else {
                current.copy(to = currency)
            }
        }
        compute(_state.value)
    }

    fun swap() {
        _state.update { it.copy(from = it.to, to = it.from) }
        compute(_state.value)
    }

    fun setInput(raw: String) {
        val filtered = raw.filter { it.isDigit() || it == ',' || it == '.' }.take(15)
        _state.update { it.copy(input = filtered) }
        compute(_state.value)
    }

    fun setQuickValue(value: Long) {
        _state.update { it.copy(input = value.toString()) }
        compute(_state.value)
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }
}
