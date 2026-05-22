# Boundless

Boundless is a data-driven questing mod for NeoForge with:

- an in-game quest book UI
- an operator-only in-game quest pack editor
- runtime settings UI
- command-based quest and questpack management

This README reflects the current implementation in `src/main`.

## What Players Get

- Quest Book item and keybind (`[` by default)
- Inventory quest button (can be disabled)
- Category/sub-category quest browsing
- Quest search
- Filters (completed/rejected/locked)
- Quest details panel with progress visualization for:
  - collect/submit
  - kill
  - effects
  - advancements
  - stats
  - xp / levelup
  - text input objectives
- Optional quest rejection flow
- Repeatable quest restart flow
- Optional quest pinning HUD
- Optional quest completion scroll support

## Operator Features

Operators (permission level `2`) can access:

- Quest Settings screen
- Quest Editor screen
- `/boundless` commands

## Questpack System

Questpacks are managed from:

- `config/boundless/questpacks/`

Pack metadata uses `pack.mcmeta` (`boundless.enabled`, optional `boundless.icon_path`).

### Enable / Disable Behavior

When a questpack is enabled/disabled from the editor UI, Boundless now runs:

- `/boundless reload`

automatically in the background so changes apply immediately.

## In-Game Quest Editor

`QuestEditorScreen` supports:

- Create/import questpacks
- Pack options editing
- Category editing
- Sub-category editing
- Quest editing
- Duplicate/delete/reorder entries
- Entry-type selectors for completion/reward rows
- Item/effect/entity/id suggestion helpers
- Dependency lock behavior controls
- Export pack zip

### Pack Options (Current Behavior)

- Pack name is editable.
- Namespace is auto-generated from pack name (invalid chars normalized).
- Pack icon id is editable (used for pack row icon display).
- On namespace changes, data folder namespace migration is handled.

## Runtime Settings UI

`QuestSettingsScreen` currently exposes grouped settings for:

- UI
- Functionality
- Gameplay

Includes controls for filter display mode, category visibility behavior, questbook visibility, pinning, auto-claim, quest scrolls, search box, toasts, and related options.

## Commands

Root command:

- `/boundless`

Key subcommands include:

- reload
- reset / complete / redeem (single or all, with targets)
- questpack list/enable/disable
- toasts enable/disable/status

Use `/boundless reload` after external datapack/quest file edits.

## Data Model (Authoring)

Boundless quest content is JSON-driven with:

- categories
- sub-categories
- quests

Quest completion target types currently include:

- `item`
- `submit`
- `entity`
- `effect`
- `advancement`
- `stat`
- `xp`
- `levelup_level`
- `field`

Reward types currently include:

- items
- commands
- functions
- loot tables
- xp/levels/levelup xp

## Multiplayer / Authority Notes

- Server state is authoritative in multiplayer.
- Player quest status/progress syncs through Boundless networking.
- Client-side UI mirrors synced state.

## Localization

Boundless uses translation keys for user-facing UI text.  
Default English strings are in:

- `src/main/resources/assets/boundless/lang/en_us.json`

## Development Notes

- For fast iteration, use questpack toggles in editor or run `/boundless reload`.
- Test both singleplayer and dedicated server behavior for new quest content.
