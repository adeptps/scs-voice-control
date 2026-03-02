package com.voicecontrol.sample

import android.Manifest
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.voicecontrol.core.CommandInterpreter
import com.voicecontrol.core.EngineMode
import com.voicecontrol.core.VoiceConfig
import com.voicecontrol.core.VoiceController
import com.voicecontrol.core.commandpack.CommandSet
import com.voicecontrol.core.commandpack.FixedPhraseInterpreter
import com.voicecontrol.core.modelpack.ModelPack
import com.voicecontrol.core.modelpack.ModelPackManager
import com.voicecontrol.core.vosk.VoskModelManager
import com.voicecontrol.core.vosk.VoskModelProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private data class UiOption(val label: String, val value: String) {
        override fun toString(): String = label
    }

    private lateinit var controller: VoiceController
    private lateinit var commandSets: List<CommandSet>
    private lateinit var modelManager: VoskModelManager
    private lateinit var modelPackManager: ModelPackManager
    private var statusTextView: TextView? = null
    private var installingArabicModel = false
    private var pendingStartAfterArabicInstall = false
    @Volatile
    private var cachedArabicPack: ModelPack? = null
    private val arabicLocale: Locale = Locale.forLanguageTag("ar")
    private val modelIndexUrl: String by lazy { getString(R.string.model_index_url).trim() }
    private val requestMicPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val status = findViewById<TextView>(R.id.txtStatus)
        status.text = if (granted) {
            getString(R.string.status_mic_permission_granted)
        } else {
            getString(R.string.status_mic_permission_denied)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)

        commandSets = loadCommandSetsFromAssets()
        val interpreter: CommandInterpreter = FixedPhraseInterpreter(commandSets = { commandSets })

        modelManager = VoskModelManager(this)
        modelPackManager = ModelPackManager(this, modelManager)
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
        val startupLocale = resolveLocaleOption(currentAppLanguageTag(), buildLocaleOptions())
        if (startupLocale.value == "ar" && modelIndexUrl.isNotBlank()) {
            ensureArabicModelInstalled(autoStartListening = false)
        }
    }

    override fun onDestroy() {
        controller.release()
        super.onDestroy()
    }

    private fun bindUi() {
        val btnMic = findViewById<MaterialButton>(R.id.btnMic)
        val txtStatus = findViewById<TextView>(R.id.txtStatus)
        statusTextView = txtStatus
        val dropdownLocale = findViewById<MaterialAutoCompleteTextView>(R.id.dropdownLocale)
        val dropdownMode = findViewById<MaterialAutoCompleteTextView>(R.id.dropdownMode)
        val chkOnline = findViewById<MaterialCheckBox>(R.id.chkOnline)
        val chkAutoOnline = findViewById<MaterialCheckBox>(R.id.chkAutoOnline)
        val localeOptions = buildLocaleOptions()
        val modeOptions = buildModeOptions()

        dropdownLocale.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, localeOptions))
        dropdownMode.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, modeOptions))
        val initialLocaleOption = resolveLocaleOption(currentAppLanguageTag(), localeOptions)
        dropdownLocale.setText(initialLocaleOption.label, false)
        dropdownMode.setText(modeOptions.first().label, false)
        dropdownLocale.setOnItemClickListener { _, _, position, _ ->
            val selected = localeOptions.getOrNull(position) ?: return@setOnItemClickListener
            applyAppLanguage(selected.value)
        }

        controller.addListener { event ->
            when (event) {
                is com.voicecontrol.core.VoiceEvent.PartialText -> txtStatus.text = event.text
                is com.voicecontrol.core.VoiceEvent.FinalText -> txtStatus.text = event.text
                is com.voicecontrol.core.VoiceEvent.CommandRecognized -> {
                    val percent = (event.command.confidence * 100f).toInt().coerceIn(0, 100)
                    txtStatus.text = getString(
                        R.string.status_command_confidence_template,
                        event.command.actionId,
                        percent,
                    )
                }
                is com.voicecontrol.core.VoiceEvent.NoCommandMatch -> {
                    val confidence = event.confidence
                    if (event.bestActionId != null && confidence != null) {
                        val percent = (confidence * 100f).toInt().coerceIn(0, 100)
                        txtStatus.text = getString(
                            R.string.status_low_confidence_template,
                            percent,
                            event.bestActionId,
                        )
                    } else {
                        txtStatus.text = getString(R.string.status_no_command_match)
                    }
                }
                is com.voicecontrol.core.VoiceEvent.Error -> {
                    txtStatus.text = getString(R.string.status_error_template, event.message)
                }
                is com.voicecontrol.core.VoiceEvent.ListeningStarted -> {
                    txtStatus.text = getString(R.string.status_listening)
                }
                is com.voicecontrol.core.VoiceEvent.ListeningStopped -> {
                    txtStatus.text = getString(R.string.status_idle)
                }
            }
        }

        btnMic.setOnClickListener {
            val localeSelection = localeOptions.firstOrNull { it.label == dropdownLocale.text.toString() }
                ?: localeOptions.first()
            val modeSelection = modeOptions.firstOrNull { it.label == dropdownMode.text.toString() }
                ?: modeOptions.first()

            val locale = Locale.forLanguageTag(localeSelection.value)
            val mode = EngineMode.valueOf(modeSelection.value)

            controller.config = VoiceConfig(
                mode = mode,
                locale = locale,
                manualOnlineEnabled = chkOnline.isChecked,
                allowAutoOnlineSwitch = chkAutoOnline.isChecked,
                enableVoskGrammar = true,
                minCommandConfidence = 0.62f,
            )

            val needsOfflineModel = locale.language == "ar" && mode != EngineMode.ONLINE_ONLY
            if (needsOfflineModel && !hasArabicModel()) {
                ensureArabicModelInstalled(autoStartListening = true)
                return@setOnClickListener
            }

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

    private fun currentAppLanguageTag(): String {
        val appTag = AppCompatDelegate.getApplicationLocales().toLanguageTags().trim()
        if (appTag.isNotBlank()) return appTag
        return resources.configuration.locales.get(0)?.toLanguageTag().orEmpty()
    }

    private fun resolveLocaleOption(tag: String, options: List<UiOption>): UiOption {
        val cleaned = tag.trim()
        val exact = options.firstOrNull { it.value.equals(cleaned, ignoreCase = true) }
        if (exact != null) return exact

        val lang = Locale.forLanguageTag(cleaned).language
        return options.firstOrNull { Locale.forLanguageTag(it.value).language == lang }
            ?: options.first()
    }

    private fun applyAppLanguage(localeTag: String) {
        val requested = LocaleListCompat.forLanguageTags(localeTag)
        val current = AppCompatDelegate.getApplicationLocales()
        if (current.toLanguageTags() == requested.toLanguageTags()) return
        AppCompatDelegate.setApplicationLocales(requested)
    }

    private fun buildLocaleOptions(): List<UiOption> {
        return listOf(
            UiOption(getString(R.string.option_locale_ar), "ar"),
            UiOption(getString(R.string.option_locale_ru), "ru-RU"),
            UiOption(getString(R.string.option_locale_en), "en-US"),
        )
    }

    private fun buildModeOptions(): List<UiOption> {
        return listOf(
            UiOption(getString(R.string.option_mode_auto), EngineMode.AUTO.name),
            UiOption(getString(R.string.option_mode_offline), EngineMode.OFFLINE_ONLY.name),
            UiOption(getString(R.string.option_mode_online), EngineMode.ONLINE_ONLY.name),
        )
    }

    private fun hasArabicModel(): Boolean {
        return modelManager.resolveModelDir(arabicLocale)?.exists() == true
    }

    private fun ensureArabicModelInstalled(autoStartListening: Boolean) {
        if (hasArabicModel()) {
            if (autoStartListening) controller.start()
            return
        }

        if (installingArabicModel) {
            if (autoStartListening) pendingStartAfterArabicInstall = true
            return
        }

        installingArabicModel = true
        if (autoStartListening) pendingStartAfterArabicInstall = true
        statusTextView?.text = getString(R.string.status_fetching_model_index)

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                var lastPercent = -1
                runCatching {
                    val pack = resolveArabicPackFromIndex()
                    statusTextView?.post {
                        statusTextView?.text = getString(R.string.status_installing_ar_model)
                    }
                    modelPackManager.installPack(pack) { downloadedBytes, totalBytes ->
                        if (totalBytes > 0) {
                            val percent = ((downloadedBytes * 100L) / totalBytes).toInt().coerceIn(0, 100)
                            if (percent != lastPercent) {
                                lastPercent = percent
                                statusTextView?.post {
                                    statusTextView?.text = getString(R.string.status_downloading_ar_model_percent, percent)
                                }
                            }
                        } else {
                            val downloadedMb = downloadedBytes / (1024 * 1024)
                            statusTextView?.post {
                                statusTextView?.text = getString(R.string.status_downloading_ar_model_mb, downloadedMb.toInt())
                            }
                        }
                    }
                }
            }

            installingArabicModel = false
            result.onSuccess {
                statusTextView?.text = getString(R.string.status_ar_model_installed)
                if (pendingStartAfterArabicInstall) {
                    pendingStartAfterArabicInstall = false
                    controller.start()
                }
            }.onFailure { err ->
                pendingStartAfterArabicInstall = false
                statusTextView?.text = getString(
                    R.string.status_ar_model_install_failed,
                    err.message ?: getString(R.string.status_check_internet)
                )
            }
        }
    }

    private fun resolveArabicPackFromIndex(): ModelPack {
        cachedArabicPack?.let { return it }
        if (modelIndexUrl.isBlank()) {
            throw IllegalStateException(getString(R.string.status_model_index_not_configured))
        }

        val catalog = modelPackManager.fetchCatalog(modelIndexUrl)
        val pack = catalog.packs
            .asSequence()
            .filter { it.engine.equals("vosk", ignoreCase = true) }
            .filter { pack ->
                pack.lang.equals("ar", ignoreCase = true) ||
                    Locale.forLanguageTag(pack.locale).language.equals("ar", ignoreCase = true)
            }
            .sortedWith(
                compareByDescending<ModelPack> { it.recommended }
                    .thenByDescending { it.version }
            )
            .firstOrNull()
            ?: throw IllegalStateException(getString(R.string.status_ar_pack_not_found_in_index))

        cachedArabicPack = pack
        return pack
    }
}
