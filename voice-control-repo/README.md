# Voice Control Android (offline Vosk + optional online SpeechRecognizer)

This repository provides an Android voice control library designed for fixed-phrase command recognition.

Key goals:
- Offline speech-to-text with Vosk
- Optional online speech-to-text using Android SpeechRecognizer
- Automatic engine selection (offline only, online only, auto)
- Downloadable model packs and command packs via an index JSON served by your backend
- Fixed phrases with exact matching for deterministic behavior
- Arabic-friendly normalization for fixed-phrase matching

## Modules

- `:voice-core`
  - Engine selection and speech recognition
  - Model pack manager (download, SHA-256 verify, install)
  - Command pack manager (download, SHA-256 verify, install)
  - Fixed phrase interpreter

- `:voice-ui`
  - Minimal helper to bind `VoiceController` to a button and status text

- `:sample-app`
  - Demonstrates how to wire everything together

## Quick start

1) Open the project in Android Studio.
2) Build and run `sample-app`.
3) Install a Vosk model pack using your backend, then test the fixed phrases.

The sample app includes a Russian command pack in assets.
This repo includes offline Vosk model ZIPs for RU, EN, and AR under `models/vosk/` and tracks them with Git LFS.

## Model packs and command packs

See:
- `docs/model-packs.md`
- `docs/command-packs.md`

## Integrating into a closed-source app

Keep the library open-source and implement your app-specific actions in a private adapter.

Recommended pattern:
- Open-source library emits `VoiceCommand(actionId, args)`
- Closed-source app maps `actionId` to internal actions

See `docs/private-adapter-example.md`.

## License

Apache License 2.0

## Offline models (Vosk)

This repo includes three offline Vosk model ZIP files in `models/vosk/`:
- `vosk-model-small-ru-0.22.zip`
- `vosk-model-small-en-us-0.15.zip`
- `vosk-model-ar-mgb2-0.4.zip`

Important:
- The Arabic model ZIP is larger than 100 MB, so Git LFS is required before pushing to GitHub.
- If you prefer not to store models in Git, delete the ZIP files and use the helper scripts in `scripts/`.

Git LFS setup (run once before the first push):

```bash
git lfs install
git lfs track "models/vosk/*.zip"
git add .gitattributes
```

Helper scripts:
- `scripts/download_vosk_model_ru_small.sh`
- `scripts/download_vosk_model_en_us_small.sh`
- `scripts/download_vosk_model_ar_mgb2.sh`
- `scripts/download_vosk_model_ru_small.ps1`
- `scripts/download_vosk_model_en_us_small.ps1`
- `scripts/download_vosk_model_ar_mgb2.ps1`

Official model links (check licenses on the model page):
- https://alphacephei.com/vosk/models

Direct links used by scripts:
- https://alphacephei.com/vosk/models/vosk-model-small-ru-0.22.zip
- https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip
- https://alphacephei.com/vosk/models/vosk-model-ar-mgb2-0.4.zip
