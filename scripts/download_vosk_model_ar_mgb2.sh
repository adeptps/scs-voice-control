#!/usr/bin/env bash
set -euo pipefail

# Downloads the official Vosk Arabic MGB2 model ZIP.
# If the official host is unavailable, it tries a mirror.

DEST_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/models/vosk"
mkdir -p "$DEST_DIR"

FILE_NAME="vosk-model-ar-mgb2-0.4.zip"
OUT="$DEST_DIR/$FILE_NAME"

URL_OFFICIAL="https://alphacephei.com/vosk/models/$FILE_NAME"
URL_MIRROR="https://huggingface.co/mychen76/vosk-models/resolve/main/ar/$FILE_NAME"

if command -v curl >/dev/null 2>&1; then
  echo "Downloading $FILE_NAME (official)..."
  if ! curl -L --fail --retry 3 --retry-delay 2 -o "$OUT" "$URL_OFFICIAL"; then
    echo "Official download failed, trying mirror..."
    curl -L --fail --retry 3 --retry-delay 2 -o "$OUT" "$URL_MIRROR"
  fi
else
  echo "curl is required"
  exit 1
fi

echo "Saved to: $OUT"
