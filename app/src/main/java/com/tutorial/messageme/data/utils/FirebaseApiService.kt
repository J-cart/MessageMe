package com.tutorial.messageme.data.utils

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tutorial.messageme.data.models.ChatMessage
import com.tutorial.messageme.data.models.PushNotifierBody
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.POST

interface FirebasePushNotifier{
    @POST("send")
   suspend fun sendMsgPush(
        @HeaderMap headers:Map<String,String>,
        @Body data:PushNotifierBody<ChatMessage>,
    ):Response<Any>
}


private const val BASE_URL = "https://fcm.googleapis.com/fcm/"

private val moshi =  Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

private val retrofit = Retrofit.Builder()
    .client(getOkHttp())
    .addConverterFactory(GsonConverterFactory.create())//MoshiConverterFactory.create(moshi)
    .baseUrl(BASE_URL)
    .build()

object ApiService{
    val retrofitApiService:FirebasePushNotifier by lazy {
        retrofit.create(FirebasePushNotifier::class.java)
    }
}

fun getOkHttp(): OkHttpClient {
    val logger = HttpLoggingInterceptor()
    logger.level = HttpLoggingInterceptor.Level.BASIC
    return OkHttpClient.Builder().addInterceptor(logger).build()
}

