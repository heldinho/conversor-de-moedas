package com.cambio.converter.data

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.core.content.edit
import com.cambio.converter.ui.MoneyFormat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.IOException

/**
 * Book de cotações sempre normalizado para base USD.
 *
 * `@Immutable` avisa o Compose que nunca alteramos esta instância depois de
 * criada — assim as subtelas que recebem o book conseguem pular recomposições.
 */
@Immutable
data class RateBook(
    val rates: Map<Currency, Double>,
    val date: String,
    val source: String,
    val fetchedAt: Long
) {
    /**
     * Rótulos prontos, calculados na thread que cria o book (IO/Default):
     * a composição só lê a String e não gasta tempo formatando data/hora.
     */
    val dateLabel: String = MoneyFormat.isoDate(date)
    val updatedLabel: String = MoneyFormat.clock(fetchedAt)

    fun rate(from: Currency, to: Currency): Double? {
        val origin = rates[from] ?: return null
        val target = rates[to] ?: return null
        if (origin <= 0.0) return null
        return target / origin
    }

    fun isFresh(maxAgeMillis: Long = FRESH_WINDOW): Boolean =
        System.currentTimeMillis() - fetchedAt < maxAgeMillis

    companion object {
        const val FRESH_WINDOW = 6 * 60 * 60 * 1000L

        fun fromUsdBase(raw: Map<String, Double>, date: String, source: String): RateBook? {
            val normalized = mutableMapOf(Currency.USD to 1.0)
            for (currency in Currency.entries) {
                if (currency == Currency.USD) continue
                val value = raw[currency.code] ?: return null
                if (value <= 0.0) return null
                normalized[currency] = value
            }
            return RateBook(normalized, date, source, System.currentTimeMillis())
        }
    }
}

private interface FrankfurterApi {
    @GET("v1/latest")
    suspend fun latest(
        @Query("base") base: String,
        @Query("symbols") symbols: String
    ): FrankfurterResponse
}

private data class FrankfurterResponse(
    val base: String?,
    val date: String?,
    val rates: Map<String, Double>?
)

private interface ErApi {
    @GET("v6/latest/{base}")
    suspend fun latest(@Path("base") base: String): ErApiResponse
}

private data class ErApiResponse(
    val result: String?,
    val base_code: String?,
    val time_last_update_utc: String?,
    val rates: Map<String, Double>?
)

private data class CacheEntry(
    val rates: Map<String, Double>,
    val date: String,
    val source: String,
    val fetchedAt: Long
)

class RateRepository(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("rate_cache", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val frankfurter: FrankfurterApi = Retrofit.Builder()
        .baseUrl("https://api.frankfurter.dev/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(FrankfurterApi::class.java)

    private val erApi: ErApi = Retrofit.Builder()
        .baseUrl("https://open.er-api.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ErApi::class.java)

    private val symbols = Currency.entries
        .filter { it != Currency.USD }
        .joinToString(",") { it.code }

    /** Fontes gratuitas em ordem de prioridade — nenhuma exige chave de API. */
    suspend fun fetch(): Result<RateBook> = withContext(Dispatchers.IO) {
        attempt { fromFrankfurter() }
            ?: attempt { fromErApi() }
            ?: Result.failure(IOException("sem conexão"))
    }

    private suspend fun fromFrankfurter(): RateBook {
        val response = frankfurter.latest(Currency.USD.code, symbols)
        return RateBook.fromUsdBase(
            raw = response.rates ?: error("resposta vazia"),
            date = response.date.orEmpty(),
            source = "Frankfurter · BCE"
        ) ?: error("cotações incompletas")
    }

    private suspend fun fromErApi(): RateBook {
        val response = erApi.latest(Currency.USD.code)
        check(response.result == "success") { "falha na fonte" }
        return RateBook.fromUsdBase(
            raw = response.rates ?: error("resposta vazia"),
            date = response.time_last_update_utc.orEmpty(),
            source = "ExchangeRate-API"
        ) ?: error("cotações incompletas")
    }

    private suspend fun attempt(block: suspend () -> RateBook): Result<RateBook>? = try {
        val book = block()
        persist(book)
        Result.success(book)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        null
    }

    private fun persist(book: RateBook) {
        val entry = CacheEntry(
            rates = book.rates.mapKeys { it.key.code },
            date = book.date,
            source = book.source,
            fetchedAt = book.fetchedAt
        )
        prefs.edit { putString(KEY_BOOK, gson.toJson(entry)) }
    }

    fun cached(): RateBook? {
        val raw = prefs.getString(KEY_BOOK, null) ?: return null
        return try {
            val type = object : TypeToken<CacheEntry>() {}.type
            val entry: CacheEntry = gson.fromJson(raw, type)
            val rates = entry.rates.mapNotNull { (code, value) ->
                Currency.fromCode(code)?.let { it to value }
            }.toMap()
            if (rates.size != Currency.entries.size) return null
            RateBook(rates, entry.date, entry.source, entry.fetchedAt)
        } catch (error: Throwable) {
            null
        }
    }

    fun clearCache() = prefs.edit { remove(KEY_BOOK) }

    companion object {
        private const val KEY_BOOK = "book"
    }
}
