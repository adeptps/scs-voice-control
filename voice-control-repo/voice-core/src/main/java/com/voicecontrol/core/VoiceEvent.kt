package com.voicecontrol.core

sealed class VoiceEvent {
    data object ListeningStarted : VoiceEvent()
    data object ListeningStopped : VoiceEvent()
    data class PartialText(val text: String) : VoiceEvent()
    data class FinalText(val text: String) : VoiceEvent()
    data class CommandRecognized(val command: VoiceCommand) : VoiceEvent()
    data class Error(val message: String, val cause: Throwable? = null) : VoiceEvent()
}
