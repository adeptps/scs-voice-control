package com.voicecontrol.core.stt

import com.voicecontrol.core.VoiceConfig

interface SttEngine {
    fun start(listener: SttListener, config: VoiceConfig)
    fun stop()
}
