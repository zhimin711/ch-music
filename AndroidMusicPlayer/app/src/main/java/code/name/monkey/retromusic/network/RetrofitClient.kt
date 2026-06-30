package code.name.monkey.retromusic.network

import android.content.Context
import code.name.monkey.retromusic.App
import code.name.monkey.retromusic.BuildConfig
import code.name.monkey.retromusic.network.conversion.LyricsConverterFactory
import com.google.gson.GsonBuilder
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit


fun provideDefaultCache(): Cache? {
    val cacheDir = File(App.getContext().cacheDir.absolutePath, "/okhttp-lastfm/")
    if (cacheDir.mkdirs() || cacheDir.isDirectory) {
        return Cache(cacheDir, 1024 * 1024 * 10)
    }
    return null
}

fun logInterceptor(): Interceptor {
    val loggingInterceptor = HttpLoggingInterceptor()
    if (BuildConfig.DEBUG) {
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
    } else {
        // disable retrofit log on release
        loggingInterceptor.level = HttpLoggingInterceptor.Level.NONE
    }
    return loggingInterceptor
}

fun headerInterceptor(context: Context): Interceptor {
    return Interceptor {
        val original = it.request()
        val request = original.newBuilder()
            .header("User-Agent", context.packageName)
            .addHeader("Content-Type", "application/json; charset=utf-8")
            .method(original.method, original.body)
            .build()
        it.proceed(request)
    }
}

fun provideOkHttp(context: Context, cache: Cache): OkHttpClient {
    return OkHttpClient.Builder()
        .addNetworkInterceptor(logInterceptor())
        .addInterceptor(headerInterceptor(context))
        .connectTimeout(1, TimeUnit.SECONDS)
        .readTimeout(1, TimeUnit.SECONDS)
        .cache(cache)
        .build()
}

fun provideLastFmRetrofit(client: OkHttpClient): Retrofit {
    @Suppress("DEPRECATION")
    val gson = GsonBuilder()
        .setLenient()
        .create()
    return Retrofit.Builder()
        .baseUrl("https://ws.audioscrobbler.com/2.0/")
        .addConverterFactory(GsonConverterFactory.create(gson))
        .callFactory { request -> client.newCall(request) }
        .build()
}

fun provideLastFmRest(retrofit: Retrofit): LastFMService {
    return retrofit.create(LastFMService::class.java)
}

fun provideDeezerRest(retrofit: Retrofit): DeezerService {
    val newBuilder = retrofit.newBuilder()
        .baseUrl("https://api.deezer.com/")
        .build()
    return newBuilder.create(DeezerService::class.java)
}

fun provideLyrics(retrofit: Retrofit): LyricsRestService {
    val newBuilder = retrofit.newBuilder()
        .baseUrl("https://makeitpersonal.co")
        .addConverterFactory(LyricsConverterFactory())
        .build()
    return newBuilder.create(LyricsRestService::class.java)
}

// ==================== 网易云音乐 API ====================

/**
 * 网易云 API 专用拦截器：
 * - 使用浏览器风格 User-Agent，避免 Vercel 反爬虫拦截
 * - 不强行注入 Content-Type（GET 请求不需要，反而可能被拒）
 * - 加上 Accept 和 Referer 让请求更像普通客户端
 */
private fun neteaseHeaderInterceptor(): Interceptor {
    return Interceptor { chain ->
        val original = chain.request()
        val builder = original.newBuilder()
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
            )
            .header("Accept", "application/json, text/plain, */*")
            .header("Referer", "https://music.163.com/")
        chain.proceed(builder.build())
    }
}

/**
 * 提供网易云音乐 API 的 OkHttp 客户端
 * 增加超时时间以适配国内网络
 */
fun provideNeteaseOkHttp(context: Context, cache: Cache): OkHttpClient {
    return OkHttpClient.Builder()
        .addNetworkInterceptor(logInterceptor())
        .addInterceptor(neteaseHeaderInterceptor())
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
}

fun provideNeteaseRetrofit(client: OkHttpClient): Retrofit {
    val gson = com.google.gson.GsonBuilder()
        .setLenient()
        .create()
    return Retrofit.Builder()
        .baseUrl(code.name.monkey.retromusic.util.PreferenceUtil.neteaseApiBaseUrl)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .callFactory { request -> client.newCall(request) }
        .build()
}

fun provideNeteaseRest(retrofit: Retrofit): NeteaseCloudApi {
    return retrofit.create(NeteaseCloudApi::class.java)
}