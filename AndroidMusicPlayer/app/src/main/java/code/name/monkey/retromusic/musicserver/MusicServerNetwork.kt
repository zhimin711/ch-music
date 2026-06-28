package code.name.monkey.retromusic.musicserver

import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

fun provideMusicServerOkHttp(session: MusicServerSession): OkHttpClient {
    return OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(Interceptor { chain ->
            val original = chain.request()
            val builder = original.newBuilder()
            if (session.accessToken.isNotBlank()) {
                builder.header("Authorization", "Bearer ${session.accessToken}")
            }
            chain.proceed(builder.build())
        })
        .build()
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
