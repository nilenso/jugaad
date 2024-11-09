package com.nilenso.jugaad.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.nilenso.jugaad.api.JugaadSendRequest
import com.nilenso.jugaad.api.JugaadWebService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class JugaadSendWorker(appCtx: Context, workerParams: WorkerParameters): Worker(appCtx, workerParams) {
    override fun doWork(): Result {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)

        val httpClient = OkHttpClient.Builder()
        httpClient.addInterceptor(logging) // <-- this is the important line!


        val rfit = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient.build())
            .baseUrl("http://10.184.57.44:5000")
            .build()

        var svc = rfit.create(JugaadWebService::class.java)

        val resp = svc.sendMessage(
            JugaadSendRequest(msg = inputData.getString("JUGAAD_MSG")!!),
            "Token abc"
        ).execute()

        if (!resp.isSuccessful) {
            return Result.failure()
        }

        return Result.success()
    }
}