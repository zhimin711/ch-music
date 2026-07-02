package code.name.monkey.retromusic.musicserver

import code.name.monkey.retromusic.BuildConfig
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

fun provideMusicServerOkHttp(session: MusicServerSession): OkHttpClient {
    val builder = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(Interceptor { chain ->
            val original = chain.request()
            val path = original.url.encodedPath
            // /api/auth/login 和 /api/auth/register 必须以匿名身份请求；
            // 老 token 无效时若挂上 Bearer，服务端鉴权过滤器会先拒 401。
            val skipAuth = path.startsWith("/api/auth/login") ||
                path.startsWith("/api/auth/register")
            val requestBuilder = original.newBuilder()
            if (!skipAuth && session.accessToken.isNotBlank()) {
                requestBuilder.header("Authorization", "Bearer ${session.accessToken}")
            }
            chain.proceed(requestBuilder.build())
        })
    if (BuildConfig.DEBUG) {
        builder.addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
    }
    return builder.build()
}

fun provideMusicServerApi(client: OkHttpClient): MusicServerApi {
    val gson = GsonBuilder().setLenient().create()
    return Retrofit.Builder()
        .baseUrl("${MusicServerDefaults.baseUrl}/")
        .addConverterFactory(GsonConverterFactory.create(gson))
        .client(client)
        .build()
        .create(MusicServerApi::class.java)
}

fun Throwable.isUnauthorized(): Boolean = this is HttpException && code() == 401

/**
 * 从 HttpException 的响应体中解析服务端提供的 message；
 * 服务端约定错误 body 形如：
 *   {"timestamp":"...","status":401,"error":"Unauthorized","message":"Invalid username or password","path":"..."}
 * 解析失败时回落到默认信息。
 */
fun Throwable.readableMessage(): String {
    if (this !is HttpException) return message ?: this::class.java.simpleName
    val raw = runCatching { response()?.errorBody()?.string() }.getOrNull()
    if (!raw.isNullOrBlank()) {
        val parsed = runCatching {
            com.google.gson.JsonParser.parseString(raw).asJsonObject
        }.getOrNull()
        val msg = parsed?.get("message")?.asString?.takeIf { it.isNotBlank() }
            ?: parsed?.get("error")?.asString?.takeIf { it.isNotBlank() }
        if (msg != null) return msg
    }
    return "HTTP ${code()} ${message().orEmpty()}".trim()
}
