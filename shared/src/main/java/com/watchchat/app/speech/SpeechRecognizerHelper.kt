// 共享模块：手机端与手表端共用
package com.watchchat.app.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * 系统语音识别封装：部分结果实时回填输入框，最终结果一次确认。
 */
class SpeechRecognizerHelper(
    context: Context,
    private val onPartialResult: (String) -> Unit,
    private val onFinalResult: (String) -> Unit,
    private val onStateChange: (Boolean) -> Unit,
    private val onError: (String) -> Unit
) {
    private val recognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext)

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = onStateChange(true)
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = onStateChange(false)

        override fun onError(error: Int) {
            onStateChange(false)
            onError(errorMessage(error))
        }

        override fun onResults(results: Bundle?) {
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
            if (text.isNotBlank()) onFinalResult(text)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val text = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
            if (text.isNotBlank()) onPartialResult(text)
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    fun start() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        recognizer.setRecognitionListener(listener)
        recognizer.startListening(intent)
    }

    fun cancel() {
        recognizer.cancel()
    }

    fun destroy() {
        recognizer.destroy()
    }

    private fun errorMessage(error: Int) = when (error) {
        SpeechRecognizer.ERROR_NO_MATCH -> "没有听清，请再试一次"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "没有说话，已停止聆听"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "缺少麦克风权限"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "语音识别正忙，请稍后再试"
        SpeechRecognizer.ERROR_NETWORK -> "网络不可用，语音识别失败"
        SpeechRecognizer.ERROR_CLIENT -> "语音识别服务异常，请重试"
        else -> "语音识别失败（错误码 $error）"
    }
}
