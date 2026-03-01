package com.voicecontrol.sample

import android.Manifest
import android.os.Bundle
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import com.voicecontrol.core.CommandInterpreter
import com.voicecontrol.core.EngineMode
import com.voicecontrol.core.VoiceConfig
import com.voicecontrol.core.VoiceController
import com.voicecontrol.core.commandpack.CommandSet
import com.voicecontrol.core.commandpack.FixedPhraseInterpreter
import com.voicecontrol.core.vosk.VoskModelManager
import com.voicecontrol.core.vosk.VoskModelProvider
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var controller: VoiceController
    private lateinit var commandSets: List<CommandSet>

    private val requestMicPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val status = findViewById<TextView>(R.id.txtStatus)
        status.text = if (granted) "Microphone permission granted" else "Microphone permission denied"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)

        commandSets = loadCommandSetsFromAssets()
        val interpreter: CommandInterpreter = FixedPhraseInterpreter(commandSets = { commandSets })

        val modelManager = VoskModelManager(this)
        val modelProvider = VoskModelProvider(
            modelManager = modelManager,
            grammarProvider = object : VoskModelProvider.GrammarProvider {
                override fun phrasesFor(locale: Locale): List<String> {
                    val tag = locale.toLanguageTag()
                    return commandSets.firstOrNull { it.localeTag.equals(tag, ignoreCase = true) }
                        ?.phrases()
                        .orEmpty()
                }
            }
        )

        controller = VoiceController(
            appContext = applicationContext,
            modelProvider = modelProvider,
            interpreter = interpreter,
        )

        bindUi()
    }

    override fun onDestroy() {
        controller.release()
        super.onDestroy()
    }

    private fun bindUi() {
        val btnMic = findViewById<MaterialButton>(R.id.btnMic)
        val txtStatus = findViewById<TextView>(R.id.txtStatus)
        val edtLocale = findViewById<TextInputEditText>(R.id.edtLocale)
        val edtMode = findViewById<TextInputEditText>(R.id.edtMode)
        val chkOnline = findViewById<MaterialCheckBox>(R.id.chkOnline)
        val chkAutoOnline = findViewById<MaterialCheckBox>(R.id.chkAutoOnline)

        controller.addListener { event ->
            when (event) {
                is com.voicecontrol.core.VoiceEvent.PartialText -> txtStatus.text = event.text
                is com.voicecontrol.core.VoiceEvent.FinalText -> txtStatus.text = event.text
                is com.voicecontrol.core.VoiceEvent.CommandRecognized -> txtStatus.text = "Command: ${event.command.actionId}"
                is com.voicecontrol.core.VoiceEvent.Error -> txtStatus.text = "Error: ${event.message}"
                is com.voicecontrol.core.VoiceEvent.ListeningStarted -> txtStatus.text = "Listening"
                is com.voicecontrol.core.VoiceEvent.ListeningStopped -> txtStatus.text = "Idle"
            }
        }

        btnMic.setOnClickListener {
            val localeTag = edtLocale.text?.toString()?.trim().orEmpty()
            val modeText = edtMode.text?.toString()?.trim().orEmpty()

            val locale = runCatching { Locale.forLanguageTag(localeTag) }.getOrDefault(Locale("ru", "RU"))
            val mode = runCatching { EngineMode.valueOf(modeText) }.getOrDefault(EngineMode.AUTO)

            controller.config = VoiceConfig(
                mode = mode,
                locale = locale,
                manualOnlineEnabled = chkOnline.isChecked,
                allowAutoOnlineSwitch = chkAutoOnline.isChecked,
                enableVoskGrammar = true,
            )

            controller.start()
        }

        btnMic.setOnLongClickListener {
            controller.stop()
            true
        }
    }

    private fun loadCommandSetsFromAssets(): List<CommandSet> {
        val list = mutableListOf<CommandSet>()
        val ru = assets.open("commandpacks/ru/commands.json").bufferedReader().use { it.readText() }
        list += CommandSet.fromJson(ru)

        val en = assets.open("commandpacks/en/commands.json").bufferedReader().use { it.readText() }
        list += CommandSet.fromJson(en)

        val ar = assets.open("commandpacks/ar/commands.json").bufferedReader().use { it.readText() }
        list += CommandSet.fromJson(ar)

        return list
    }
}
