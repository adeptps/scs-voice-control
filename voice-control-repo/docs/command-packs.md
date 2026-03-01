# Command packs

A command pack is a zip file containing `commands.json`.

The library supports fixed phrases with exact matching.

## commands.json format

```json
{
  "schema": 1,
  "pack_id": "ru-car-v1.1",
  "lang": "ru",
  "locale": "ru-RU",
  "version": "1.1.0",
  "generated_at": "2026-03-01T00:00:00Z",
  "commands": [
    { "id": "car.climate.on", "phrases": ["включи климат"] }
  ]
}
```

## Index format

`GET /voice/commands/v1/index.json`

Same pattern as model packs:
- app downloads index
- app downloads zip
- app verifies sha256
- app installs to `filesDir/voice_command_packs/<packId>`
