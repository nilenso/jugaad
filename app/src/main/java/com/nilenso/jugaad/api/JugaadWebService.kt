package com.nilenso.jugaad.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface JugaadWebService {
    @Headers("Content-Type: application/json;charset=UTF-8")
    @POST("/send")
    fun sendMessage(@Body request: JugaadSendRequest, @Header("Authorization") auth: String): Call<JugaadResponse>
}