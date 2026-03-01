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
            is VoiceEvent.ListeningStarted -> statusText.text = "Listening"
            is VoiceEvent.ListeningStopped -> statusText.text = "Idle"
            is VoiceEvent.PartialText -> statusText.text = e.text
            is VoiceEvent.FinalText -> statusText.text = e.text
            is VoiceEvent.CommandRecognized -> {
                statusText.text = "Command: ${e.command.actionId}"
                if (showToasts) Toast.makeText(context, "Command: ${e.command.actionId}", Toast.LENGTH_SHORT).show()
            }
            is VoiceEvent.Error -> {
                statusText.text = "Error: ${e.message}"
                if (showToasts) Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
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
