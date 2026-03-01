#!/usr/bin/env bash
set -euo pipefail

# Downloads the official Vosk small US English model and installs it for local development.
# This script is intentionally separate from the app to avoid committing large model files into Git.

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="$ROOT_DIR/.local_models"
MODEL_ZIP="$OUT_DIR/vosk-model-small-en-us-0.15.zip"
MODEL_DIR="$OUT_DIR/vosk-model-small-en-us-0.15"

mkdir -p "$OUT_DIR"

URL="https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip"

if [ ! -f "$MODEL_ZIP" ]; then
  echo "Downloading: $URL"
  curl -L --fail --retry 3 -o "$MODEL_ZIP" "$URL"
else
  echo "Already downloaded: $MODEL_ZIP"
fi

if [ ! -d "$MODEL_DIR" ]; then
  echo "Extracting to: $MODEL_DIR"
  unzip -q "$MODEL_ZIP" -d "$OUT_DIR"
else
  echo "Already extracted: $MODEL_DIR"
fi

echo "Done. Model directory: $MODEL_DIR"
