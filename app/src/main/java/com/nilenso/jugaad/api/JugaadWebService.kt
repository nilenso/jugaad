package com.nilenso.jugaad.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Url

interface JugaadWebService {
    @Headers("Content-Type: application/json;charset=UTF-8")
    @POST
    fun sendMessage(@Url url: String, @Body request: JugaadSendRequest): Call<JugaadResponse>
}
