package com.aitrader.app.api

import com.aitrader.app.AppPreferences
import com.aitrader.app.BuildConfig
import com.aitrader.app.model.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface ApiService {

    @GET("api/status")
    suspend fun getStatus(@Query("portfolio") portfolio: String? = null): StatusResponse

    @GET("api/prices")
    suspend fun getPrices(): PricesResponse

    @GET("api/market-regime")
    suspend fun getMarketRegime(): RegimeResponse

    @GET("api/candles")
    suspend fun getCandles(
        @Query("symbol") symbol: String,
        @Query("timeframe") timeframe: String,
        @Query("limit") limit: Int = 300,
    ): CandlesResponse

    @GET("api/scan")
    suspend fun runScan(): ScanResponse

    @GET("api/ai-scan")
    suspend fun runAiScan(): ScanResponse

    @GET("api/scan/status/{job_id}")
    suspend fun getScanJobStatus(@Path("job_id") jobId: String): ScanJobStatusResponse

    @POST("api/order")
    suspend fun placeOrder(@Body request: OrderRequest): OrderResponse

    @POST("api/trade")
    suspend fun runTrade(@Body request: TradeRequest): TradeResponse

    @POST("api/trade")
    suspend fun executeTrade(@Body request: DirectTradeRequest): DirectTradeResponse

    @POST("api/ai-signals/apply")
    suspend fun applyAiSignals(@Body request: ApplySignalsRequest): TradeResponse

    @POST("api/autopilot/start")
    suspend fun startAutopilot(@Body request: AutopilotRequest): AutopilotResponse

    @POST("api/autopilot/stop")
    suspend fun stopAutopilot(): AutopilotResponse

    @POST("api/watchlist/scan")
    suspend fun forceWatchlistScan(): WatchlistResponse

    @GET("api/watchlist")
    suspend fun getWatchlist(): WatchlistResponse

    @GET("api/journal")
    suspend fun getJournal(@Query("portfolio") portfolio: String? = null): JournalResponse

    @GET("api/logs/dates")
    suspend fun getLogDates(): LogDatesResponse

    @GET("api/logs/recent")
    suspend fun getRecentLogs(
        @Query("lines") lines: Int = 500,
        @Query("date") date: String? = null
    ): LogsResponse

    @GET("api/training-log")
    suspend fun getTrainingLog(): Map<String, Any>

    @POST("api/chat")
    suspend fun chat(@Body request: ChatRequest): ChatResponse

    @GET("api/chat/status/{job_id}")
    suspend fun getChatJobStatus(@Path("job_id") jobId: String): ChatJobStatusResponse

    companion object {
        private var instance: ApiService? = null
        private var lastUrl: String? = null

        @Synchronized
        fun create(baseUrl: String = AppPreferences.baseUrl): ApiService {
            // Re-create if URL changed
            if (instance != null && lastUrl == baseUrl) return instance!!

            val logging = HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
                        else HttpLoggingInterceptor.Level.NONE
            }

            val client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val original = chain.request()
                    val key = AppPreferences.apiKey.trim()
                    val builder = original.newBuilder()

                    // Add API key header
                    if (key.isNotEmpty()) {
                        builder.addHeader("X-API-Key", key)
                    }

                    chain.proceed(builder.build())
                }
                .addInterceptor(logging)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(180, TimeUnit.SECONDS)
                .writeTimeout(180, TimeUnit.SECONDS)
                .build()

            val url = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

            instance = Retrofit.Builder()
                .baseUrl(url)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
            lastUrl = baseUrl

            return instance!!
        }

        /** Force recreate on next call (e.g., after URL change) */
        fun invalidate() {
            instance = null
            lastUrl = null
        }
    }
}
