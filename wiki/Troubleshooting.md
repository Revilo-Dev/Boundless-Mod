# Troubleshooting

## Quest Not Progressing

Check:

- Correct objective type
- Exact target id
- Required count/threshold
- Dependencies completed first

Then run:

`/boundless reload`

## Quest Pack Not Showing

Check:

- Folder is under `config/boundless/questpacks/`
- Pack has valid multi-JSON data structure
- Pack is enabled
- JSON is valid (no syntax errors)

## UI Button/Keybind Issues

Check:

- Keybind conflicts
- Questbook visibility settings
- Inventory button setting state

## Multiplayer Desync Symptoms

- Quest progression authority is server-side.
- Re-test after a server reload.
- Confirm server and client run matching mod versions.

## Editor Changes Not Reflected

- Save the entry
- Reload (`/boundless reload`)
- Reopen the quest screen

## Hard Failures

If the game logs parsing or load errors:

1. Isolate the changed files.
2. Validate JSON formatting.
3. Re-add changes incrementally to identify the bad file.
