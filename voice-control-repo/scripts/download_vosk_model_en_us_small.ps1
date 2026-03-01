# Downloads the official Vosk small US English model and installs it for local development.
# This script is intentionally separate from the app to avoid committing large model files into Git.

$ErrorActionPreference = "Stop"

$RootDir = Resolve-Path (Join-Path $PSScriptRoot "..")
$OutDir = Join-Path $RootDir ".local_models"
$ModelZip = Join-Path $OutDir "vosk-model-small-en-us-0.15.zip"
$ModelDir = Join-Path $OutDir "vosk-model-small-en-us-0.15"

New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

$Url = "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip"

if (-not (Test-Path $ModelZip)) {
  Write-Host "Downloading: $Url"
  Invoke-WebRequest -Uri $Url -OutFile $ModelZip
} else {
  Write-Host "Already downloaded: $ModelZip"
}

if (-not (Test-Path $ModelDir)) {
  Write-Host "Extracting to: $ModelDir"
  Expand-Archive -Path $ModelZip -DestinationPath $OutDir
} else {
  Write-Host "Already extracted: $ModelDir"
}

Write-Host "Done. Model directory: $ModelDir"
