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
    /**
     * 流式请求必须声明 Accept: text/event-stream，
     * 服务端才会以 SSE 格式（data: {...} 行）逐块返回。
     */
    @Streaming
    @POST("chat/completions")
    suspend fun streamChat(
        @Header("Authorization") authorization: String,
        @Header("Accept") accept: String,
        @Body body: ChatRequest
    ): Response<ResponseBody>

    @POST("chat/completions")
    suspend fun chat(
        @Header("Authorization") authorization: String,
        @Header("Accept") accept: String,
        @Body body: ChatRequest
    ): Response<ChatCompletion>
}
