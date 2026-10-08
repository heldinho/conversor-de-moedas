package com.cambio.converter

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cambio.converter.data.Currency
import com.cambio.converter.data.RateBook
import com.cambio.converter.data.RateRepository
import com.cambio.converter.ui.MoneyFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConverterUiState(
    val from: Currency = Currency.USD,
    val to: Currency = Currency.BRL,
    val input: String = "1",
    val book: RateBook? = null,
    val isLoading: Boolean = false,
    val hasNetworkError: Boolean = false,
    val message: String? = null
) {
    val rate: Double? = book?.rate(from, to)
    val amount: Double get() = MoneyFormat.parse(input) ?: 0.0
    val result: Double get() = rate?.let { amount * it } ?: 0.0
    val isLive: Boolean get() = book != null && !hasNetworkError
}

class ConverterViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RateRepository(application)

    private val _state = MutableStateFlow(ConverterUiState(book = repository.cached()))
    val state: StateFlow<ConverterUiState> = _state.asStateFlow()

    init {
        refresh()
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
    }

    fun setTo(currency: Currency) {
        _state.update { current ->
            if (currency == current.from) {
                current.copy(from = current.to, to = currency)
            } else {
                current.copy(to = currency)
            }
        }
    }

    fun swap() {
        _state.update { it.copy(from = it.to, to = it.from) }
    }

    fun setInput(raw: String) {
        val filtered = raw.filter { it.isDigit() || it == ',' || it == '.' }.take(15)
        _state.update { it.copy(input = filtered) }
    }

    fun setQuickValue(value: Long) {
        _state.update { it.copy(input = value.toString()) }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }
}
