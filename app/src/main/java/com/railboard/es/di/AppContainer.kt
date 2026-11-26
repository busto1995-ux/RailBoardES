package com.railboard.es.di

import android.content.Context
import com.railboard.es.BuildConfig
import com.railboard.es.data.local.UserPreferencesDataStore
import com.railboard.es.data.remote.MockRailDataSource
import com.railboard.es.data.remote.RailApiService
import com.railboard.es.data.remote.RailDataSource
import com.railboard.es.data.remote.RetrofitRailDataSource
import com.railboard.es.data.repository.RailRepository
import com.railboard.es.location.NearestStationLocator
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {

    private val appContext = context.applicationContext
    private val preferences = UserPreferencesDataStore(appContext)
    private val mockDataSource = MockRailDataSource()

    private val remoteDataSource: RailDataSource? by lazy {
        val baseUrl = BuildConfig.RAIL_BFF_BASE_URL.trim()
        if (baseUrl.isBlank()) return@lazy null

        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        val okHttp = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        val api = Retrofit.Builder()
            .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RailApiService::class.java)

        RetrofitRailDataSource(api)
    }

    val repository: RailRepository by lazy {
        RailRepository(
            remoteDataSource = remoteDataSource,
            mockDataSource = mockDataSource,
            prefs = preferences
        )
    }

    val stationLocator: NearestStationLocator by lazy {
        NearestStationLocator(appContext)
    }
}
