# Boundless

Boundless is a data-driven questing mod for NeoForge with an in-game quest editor and runtime config system.

This README documents the **editor**, **config**, and **quest-pack authoring** features in the current codebase (`src/main`).

## Quick Access

- Open quest book keybind: `[` (default), configurable in controls.
- Inventory quest button: shown unless disabled by config.
- Standalone quest screen: available from quest book item/keybind.
- Settings + Editor screen: available to players with permission level `2` (operators).

## Editor Overview

The in-game editor (`QuestEditorScreen`) is a pack-author workflow for creating and maintaining quest packs without leaving Minecraft.

Main areas:

1. **Quest Pack List**
2. **Pack Create / Pack Options**
3. **Category Editor**
4. **Sub-Category Editor**
5. **Quest Editor**

Core editor actions:

- Create new quest pack
- Import quest pack (zip)
- Enable/disable packs
- Duplicate entries
- Delete entries (with confirm arm)
- Reorder entries
- Save edits to disk
- Export quest pack (zip)
- Open pack directory
- Convert pack to new format

The editor also includes:

- ID sanitizing and validation
- Suggestion lists for IDs/registry values
- Dynamic row editors for dependencies/completion/rewards
- Lock toggles for dependency behavior
- Unsaved state handling while navigating

## Quest Pack Storage and Paths

Instance quest packs are managed under:

- `config/boundless/questpacks/`

Boundless also loads from datapack/resource-pack quest JSON paths:

- `data/<namespace>/quests/*.json`
- `data/<namespace>/quests/categories/*.json`
- `data/<namespace>/quests/subcategories/*.json`
- plus compatibility aliases:
  - `sub_category`
  - `subcategory`
  - `sub-category`

Built-in quest pack loading can be disabled via config.

## Config File

Boundless registers a common config:

- `boundless-common.toml`

It is grouped into `UI`, `Functionality`, and `Gameplay`.

### UI

- `disabledQuestCategories` (`List<String>`)
  - Completely hides quests in matching category IDs.
- `pinnedQuestHudPosition` (`top_left | top_right | bottom_left | bottom_right`)
- `hideQuestBookInInventory` (`boolean`)
- `hideCategoryHeader` (`boolean`)
- `filterDisplayMode` (`tabs | buttons | hidden`)
- `disableCategories` (`boolean`)
- `enableBuiltinQuestPack` (`boolean`)
- `hideQuestWidgetIcons` (`boolean`)
- `enableQuestSearchBox` (`boolean`)
- `enableDescriptionColors` (`boolean`)
- `enableQuestToasts` (`boolean`)

### Functionality

- `disableQuestPinning` (`boolean`)
- `autoClaimQuestRewards` (`boolean`)
- `enableQuestScrolls` (`boolean`)
- `datapackQuestPacksOnlyOnServer` (`boolean`)

### Gameplay

- `disableQuestBook` (`boolean`)
- `spawnWithQuestBook` (`boolean`)

## In-Game Settings Screen

`QuestSettingsScreen` provides an operator-facing UI for most runtime options.

Config screen sections:

1. **UI**
2. **Functionality**
3. **Gameplay**

Editable from the screen:

- Pinned quest HUD position
- Hide quest book button in inventory
- Hide category header
- Filter display mode
- Disable categories
- Hide quest widget icons
- Enable quest search box
- Enable description colors
- Enable quest toasts
- Disable quest pinning
- Auto-claim rewards
- Enable quest scrolls
- Disable quest book
- Spawn with quest book

Saved via `Config.SPEC.save()` and immediately applied to open UI panels.

## Command Reference

Root command: `/boundless` (requires permission level `2`)

- `/boundless reload`
  - Reloads quest data and re-syncs players.
- `/boundless reset all [targets]`
- `/boundless reset <quest_id> [targets]`
- `/boundless complete all [targets]`
- `/boundless complete <quest_id> [targets]`
- `/boundless redeem all [targets]`
- `/boundless redeem <quest_id> [targets]`
- `/boundless toasts enable|disable|status`
- `/boundless questpack list`
- `/boundless questpack enable <id>`
- `/boundless questpack disable <id>`

## Quest JSON Model

### Category

Typical fields:

- `id` (required)
- `name`
- `icon`
- `order`
- `exclude_from_all`
- `dependency`
- `auto_complete` / `autoComplete`

### Sub-Category

Typical fields:

- `id`
- `category`
- `name`
- `icon`
- `order`
- `defaultOpen`
- `quests` (list of quest IDs)

### Quest

Typical fields:

- `id` (required)
- `name`
- `icon`
- `description`
- `category`
- `subCategory`
- `dependencies` (`List<String>`)
- `lock_after_dependency` / `lockAfterDependency`
- `optional`
- `repeatable`
- `auto_complete` / `autoComplete`
- `hiddenUnderDependency`
- `type`
- `completion`
- `rewards`

### Completion Target Kinds

Boundless supports:

- `item`
- `submit`
- `entity`
- `effect`
- `advancement`
- `stat`
- `xp`
- `levelup_level`
- `field`

### Reward Types

- Item rewards (`items`)
- Command rewards (`command`/`commands`)
- Function rewards (`functions`)
- Loot table rewards (`lootTables`)
- XP rewards (`expType` + `expAmount`)

## Quest Pack Enable/Disable Metadata

Quest pack enabled state is persisted in the pack `pack.mcmeta`:

- `boundless.enabled: true|false`

This is used by both command-based and editor-based pack toggling.

## Permission and Multiplayer Notes

- Settings/editor access is intentionally operator-gated (`hasPermissions(2)`).
- Server-side quest data is authoritative in multiplayer.
- On login/reload, quest data and player state sync through Boundless network payloads.

## Notes for Pack Authors

1. Keep quest IDs stable once published.
2. Validate all registry IDs (`item`, `entity`, `effect`, `advancement`, `loot table`).
3. Use clear dependency chains and only enable `hiddenUnderDependency` where needed.
4. Test both singleplayer and dedicated server behavior.
5. Use `/boundless reload` after data changes during iteration.

