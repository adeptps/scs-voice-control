package com.voicecontrol.core.stt

interface SttListener {
    fun onPartial(text: String)
    fun onFinal(text: String)
    fun onError(message: String, cause: Throwable? = null)
}
