# Private adapter example

The open-source library emits `VoiceCommand(actionId, args)`.

Your closed-source app should implement the action mapping.

Example interface:

```kotlin
interface VoiceActionHandler {
    fun handle(actionId: String, args: Map<String, String>): Boolean
}
```

Suggested flow:
1) Create `VoiceController` with `FixedPhraseInterpreter`.
2) When a command is recognized, call your `VoiceActionHandler`.
3) If the handler returns true, play a click sound and show a toast.
4) Keep paid features behind your subscription checks.
