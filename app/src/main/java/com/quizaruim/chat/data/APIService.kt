package com.quizaruim.chat.data

import com.quizaruim.chat.utils.Constants
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

interface APIService {
    @POST("install")
    suspend fun sendInfoInstall(@Body body: RequestBody) : Response<ResponseBody>

    @POST("appsflyerid")
    suspend fun sendAppsflyerID(@Body body: RequestBody) : Response<ResponseBody>

    @POST("deeplink")
    suspend fun sendDeepLink(@Body body: RequestBody) : Response<ResponseBody>

    @POST("fcm")
    suspend fun sendFirebaseToken(@Body body: RequestBody) : Response<ResponseBody>

    companion object {
        val retrofit by lazy {
            Retrofit.Builder()
                .baseUrl(Constants.URL_API)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(APIService::class.java)
        }
    }
}