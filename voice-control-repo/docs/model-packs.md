# Model packs

A model pack is a zip file containing a Vosk model directory.
The app downloads model packs from URLs listed in an index JSON.

The index is served by your backend. The backend may respond with a 302 redirect to a storage provider.

## Index format

`GET /models/voice/v1/index.json`

Example:

```json
{
  "schema": 1,
  "generated_at": "2026-03-01T00:00:00Z",
  "packs": [
    {
      "id": "vosk-ru-small-0.22",
      "engine": "vosk",
      "lang": "ru",
      "locale": "ru-RU",
      "variant": "small",
      "version": "0.22",
      "url": "https://YOUR_BACKEND/models/voice/v1/packs/vosk-ru-small-0.22.zip",
      "size_bytes": 47123456,
      "sha256": "lowercase_hex_sha256",
      "license": "Apache-2.0",
      "installed_dir": "model-ru-ru",
      "min_sdk": 23,
      "recommended": true
    }
  ]
}
```

## Zip structure

The zip should unpack into the model directory contents.
Example installed path:

`filesDir/vosk_models/model-ru-ru/...`

Security note:
- The library rejects path traversal in zips.

## Recommended pack list (RU/EN/AR)

Example (fill your backend domain and pack URLs):

```json
{
  "schema": 1,
  "generated_at": "2026-03-01T00:00:00Z",
  "packs": [
    {
      "id": "vosk-ru-small-0.22",
      "engine": "vosk",
      "lang": "ru",
      "locale": "ru-RU",
      "variant": "small",
      "version": "0.22",
      "url": "https://YOUR_BACKEND/models/voice/v1/packs/vosk-ru-small-0.22.zip",
      "size_bytes": 46236750,
      "sha256": "961d5ff98a17f4aa6de69864d0aa71fa5bac682301d2b5d17a3f24c5c99a46d4",
      "license": "Apache-2.0",
      "installed_dir": "model-ru-ru",
      "min_sdk": 23,
      "recommended": true
    },
    {
      "id": "vosk-en-us-small-0.15",
      "engine": "vosk",
      "lang": "en",
      "locale": "en-US",
      "variant": "small",
      "version": "0.15",
      "url": "https://YOUR_BACKEND/models/voice/v1/packs/vosk-en-us-small-0.15.zip",
      "size_bytes": 41205931,
      "sha256": "30f26242c4eb449f948e42cb302dd7a686cb29a3423a8367f99ff41780942498",
      "license": "Apache-2.0",
      "installed_dir": "model-en-us",
      "min_sdk": 23,
      "recommended": true
    },
    {
      "id": "vosk-ar-mgb2-0.4",
      "engine": "vosk",
      "lang": "ar",
      "locale": "ar",
      "variant": "full",
      "version": "0.4",
      "url": "https://YOUR_BACKEND/models/voice/v1/packs/vosk-ar-mgb2-0.4.zip",
      "size_bytes": 333241610,
      "sha256": "357469ae1bb4d7a3810c9cd6b86d33bc135898dfc134e6df8bc2ddd28c5fe77a",
      "license": "Apache-2.0",
      "installed_dir": "model-ar",
      "min_sdk": 23,
      "recommended": false
    }
  ]
}
```

Note:
- The Arabic model is large. Consider making it optional and downloading it only when the user selects Arabic.
