# SCS Voice Command Execution API (Private)

This document describes how the open-source voice module should hand over recognized commands to the SCS application.

## Goal

- Keep the voice module generic (ASR + fixed-phrase command matching).
- Keep all car-specific logic inside SCS.
- The voice module must only output a `commandId` (string) plus optional metadata.
- SCS executes the command using DirectCarClient (DIRECT channel).

## Entry Point in SCS

SCS exposes a private Kotlin API:

```kotlin
package com.example.climateseats.voice

object VoiceCommandApi {
    data class ExecResult(
        val ok: Boolean,
        val message: String,
        val executedCommandId: String,
    )

    /** Executes a command id and provides user feedback (click sound + toast). */
    fun execute(context: Context, commandId: String): ExecResult

    /** Returns a compact JSON with the API version and supported transport. */
    fun getApiInfoJson(): String
}
```

### Behavior

- Plays the UI click sound (same as UI buttons).
- Executes the command via `DirectCarClient` using known function IDs.
- Shows a short toast with the result.
- Returns `ExecResult` for logging or UI.

## What the Voice Module Should Provide

### Required

- `commandId`: stable identifier, example: `car.window.driver.50`

### Optional (recommended)

- `recognizedText`: raw speech text returned by ASR
- `confidence`: command match confidence (0..1)
- `localeTag`: example: `ru-RU`, `en-US`, `ar`

SCS does not need these fields to execute the command, but they are useful for logging and debugging.

## Recommended Integration Pattern

The voice module should expose an event/callback for recognized commands, for example:

```kotlin
interface VoiceCommandListener {
    fun onCommandRecognized(
        commandId: String,
        recognizedText: String?,
        confidence: Float?,
        localeTag: String?
    )
}
```

In SCS, implement this callback and execute the command:

```kotlin
val listener = object : VoiceCommandListener {
    override fun onCommandRecognized(commandId: String, recognizedText: String?, confidence: Float?, localeTag: String?) {
        VoiceCommandApi.execute(appContext, commandId)
    }
}
```

If the voice module supports a minimum confidence threshold, it should NOT call `onCommandRecognized` when the match is below threshold.
Instead it should report a separate event (example: `NoCommandMatch`).

## Building SCS With or Without the Voice Module

SCS must be buildable without any voice recognition code.

- Default: voice module is disabled, the app compiles and runs normally.
- Developer build: enable voice module via `local.properties` in the SCS root.

Example `local.properties`:

```
voice.module.enabled=true
voice.module.dir=C:\path\to\scs-voice-control
```

When enabled, SCS includes `:voice-core` and `:voice-ui` from the external path.

## Developer Test Page (Web UI Bridge)

SCS provides a hidden test page for voice control.
Open: About page -> tap the version field 5 times.

The web page uses the Android JS bridge:

- `Android.startVoiceControl(localeTag, mode, manualOnlineEnabled, allowAutoOnlineSwitch, enableVoskGrammar)`
- `Android.voiceListenOnce()`
- `Android.stopVoiceControl()`
- `Android.getVoiceControlStatusJson()`
- `Android.pingVoiceControlApi()`
- `Android.getVoiceCommandListJson(localeTag)`
- `Android.getVoicePhraseListJson(localeTag)`

### Await Command (One-shot Listening)

Some voice engines separate initialization from the actual listening cycle.
The developer test page provides a dedicated "Await command" button.
It calls `Android.voiceListenOnce()`.

Required behavior:

- If the voice engine supports a one-shot listening cycle, start listening until final result or timeout.
- If it does not support one-shot listening, the implementation may fallback to `startVoiceControl()`.
- The engine must avoid concurrent listening sessions.

## Command IDs

SCS uses fixed-phrase command packs. The IDs must be identical across languages.

Examples:

- Windows: `car.window.driver.0..100`, `car.window.all_close`, `car.window.all_open`, `car.window.all_open_half`
- Sunroof: `car.roof.0..100`, `car.shade.0..100`
- Trunk: `car.trunk.open`, `car.trunk.close`
- Climate: `car.climate.on`, `car.climate.off`, `car.climate.auto.on`, `car.climate.ac.on`
- Temperature: commands are defined in command packs (for this vehicle SCS uses 1°C step)
- Seats: `car.seat_heat.driver.0..3`, `car.seat_vent.driver.0..3`, `car.steering_heat.0..3`

The authoritative mapping is implemented in:

`app/src/main/java/com/example/climateseats/voice/VoiceCommandApi.kt`

## Notes

- SCS executes commands via DIRECT channel. Function IDs match the IDs visible in GIB tools.
- Central lock control can be read via `id=553779712`, but writing may be blocked by firmware. SCS will send a best-effort SET and report the result.
