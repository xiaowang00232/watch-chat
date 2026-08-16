// 共享模块：手机端与手表端共用
package com.watchchat.app.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Streaming

/** 兼容 OpenAI Chat Completions 格式的接口（OpenAI / DeepSeek / 通义等）。 */
interface OpenAiApi {
    @Streaming
    @POST("chat/completions")
    suspend fun streamChat(
        @Header("Authorization") authorization: String,
        @Body body: ChatRequest
    ): Response<ResponseBody>

    @POST("chat/completions")
    suspend fun chat(
        @Header("Authorization") authorization: String,
        @Body body: ChatRequest
    ): Response<ChatCompletion>
}
