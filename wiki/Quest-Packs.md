# Quest Packs

## Pack Location

Quest packs are loaded from:

`config/boundless/questpacks/`

Each pack should be a folder with a valid data layout.

## Multi-JSON Structure (Current)

Boundless uses the multi-file data structure:

`data/<namespace>/quests/`

Inside that:

- `categories/*.json`
- `sub-category/*.json` (also supports related subcategory directory variants)
- quest files directly under `quests/*.json`

## Metadata

Pack metadata is read from `pack.mcmeta` with Boundless fields such as enabled state and optional icon path.

## Enable/Disable

You can enable/disable packs using:

- In-game editor toggles (operator use)
- `/boundless questpack` commands

After state changes, Boundless reloads quest data.

## Importing and Exporting

The in-game editor supports:

- Opening import directory
- Exporting a quest pack zip

## Reloading After Manual Edits

If you edit files outside the game:

`/boundless reload`

This refreshes quest data from disk.
