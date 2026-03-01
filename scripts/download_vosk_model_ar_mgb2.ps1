# Downloads the official Vosk Arabic MGB2 model ZIP.
# If the official host is unavailable, it tries a mirror.

$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$DestDir = Join-Path $RepoRoot "models\vosk"
New-Item -ItemType Directory -Force -Path $DestDir | Out-Null

$FileName = "vosk-model-ar-mgb2-0.4.zip"
$OutFile = Join-Path $DestDir $FileName

$UrlOfficial = "https://alphacephei.com/vosk/models/$FileName"
$UrlMirror   = "https://huggingface.co/mychen76/vosk-models/resolve/main/ar/$FileName"

Write-Host "Downloading $FileName (official)..."
try {
  Invoke-WebRequest -Uri $UrlOfficial -OutFile $OutFile -UseBasicParsing
} catch {
  Write-Host "Official download failed, trying mirror..."
  Invoke-WebRequest -Uri $UrlMirror -OutFile $OutFile -UseBasicParsing
}

Write-Host "Saved to: $OutFile"
