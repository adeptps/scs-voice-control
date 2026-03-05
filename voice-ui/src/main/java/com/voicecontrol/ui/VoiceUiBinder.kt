package com.voicecontrol.ui

import android.content.Context
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.voicecontrol.core.VoiceController
import com.voicecontrol.core.VoiceEvent

/**
 * Convenience binder for simple UI integration.
 */
class VoiceUiBinder(
    private val context: Context,
    private val controller: VoiceController,
    private val micButton: View,
    private val statusText: TextView,
    private val showToasts: Boolean = true,
) {

    private val listener: (VoiceEvent) -> Unit = { e ->
        when (e) {
            is VoiceEvent.ListeningStarted -> statusText.text = context.getString(R.string.voice_ui_status_listening)
            is VoiceEvent.ListeningStopped -> statusText.text = context.getString(R.string.voice_ui_status_idle)
            is VoiceEvent.PartialText -> statusText.text = e.text
            is VoiceEvent.FinalText -> statusText.text = e.text
            is VoiceEvent.CommandRecognized -> {
                val percent = (e.command.confidence * 100f).toInt().coerceIn(0, 100)
                val message = context.getString(
                    R.string.voice_ui_status_command_confidence,
                    e.command.actionId,
                    percent,
                )
                statusText.text = message
                if (showToasts) Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
            is VoiceEvent.NoCommandMatch -> {
                val bestActionId = e.bestActionId
                val confidence = e.confidence
                val message = if (bestActionId != null && confidence != null) {
                    val percent = (confidence * 100f).toInt().coerceIn(0, 100)
                    context.getString(R.string.voice_ui_status_low_confidence, percent, bestActionId)
                } else {
                    context.getString(R.string.voice_ui_status_no_match)
                }
                statusText.text = message
            }
            is VoiceEvent.Error -> {
                val message = context.getString(R.string.voice_ui_status_error, e.message)
                statusText.text = message
                if (showToasts) Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun bind() {
        controller.addListener(listener)
        micButton.setOnClickListener {
            controller.start()
        }
        micButton.setOnLongClickListener {
            controller.stop()
            true
        }
    }

    fun unbind() {
        controller.removeListener(listener)
        micButton.setOnClickListener(null)
        micButton.setOnLongClickListener(null)
    }
}
