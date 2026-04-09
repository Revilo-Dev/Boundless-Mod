# Boundless

Boundless is a data-driven questing mod for Minecraft modpacks. It adds quest books, categories, sub-categories, completion tracking, rewards, optional quests, repeatable quests, quest scrolls, and an in-game editor for building or adjusting quest packs.

The mod is built around simple JSON quest data and a client/server sync layer. Pack authors can ship quests inside data packs or resource packs, and players can browse, complete, redeem, repeat, pin, and filter quests from the in-game UI.

## What The Mod Does

Boundless provides:

- A quest book UI that can open in-game and as a standalone screen
- Category and sub-category organisation for large quest trees
- Multiple quest objective types
- Multiple reward types
- Dependency-based quest progression
- Optional and repeatable quest support
- Quest pinning and quest scroll creation
- In-game quest editing and quest pack management
- Client and server sync for progress, claim counts, kill counts, and quest metadata

## Core Concepts

### Categories

Categories are the top-level organisation for quests. They define the main sections of the quest UI and can have:

- An ID
- A display name
- An icon
- An order
- An optional dependency
- An option to exclude them from the combined "all" view

Categories are useful for separating progression into themes such as early game, automation, magic, exploration, or bossing.

### Sub-Categories

Sub-categories sit inside a category and group related quests together. They are useful when one category becomes too large and needs more structure. A sub-category can have:

- An ID
- A parent category
- A display name
- An icon
- An order
- A default open state
- An explicit quest list

### Quests

A quest can contain:

- A unique ID
- A title
- An icon
- A description
- A category
- An optional sub-category
- Dependencies on other quests
- Flags such as `optional`, `repeatable`, and `hiddenUnderDependency`
- Completion targets
- Rewards

## Supported Objective Types

Boundless supports several completion target types:

- `item`: collect and keep items in inventory
- `submit`: hand in items from inventory
- `entity`: kill entities
- `effect`: gain or have a potion/effect
- `advancement`: complete a Minecraft advancement
- `stat`: reach a stat value
- `xp`: hold or submit XP points or levels

### Collection vs Submission

Collection targets only require the player to possess the required item or resource. Submission targets consume the required items or XP during claim.

Legacy submission-style quests are also supported through quest type handling, so older quest data can still behave correctly.

## Rewards

Quests can grant several reward types:

- Item rewards
- Command rewards
- Function rewards
- Loot table rewards
- XP rewards

If an item reward cannot be resolved from its item ID, it is skipped instead of crashing the claim flow. This keeps malformed reward data from blocking other rewards or the final redeem state.

## Progression Rules

### Dependencies

Quests can depend on other quests. A dependency is considered satisfied when the required quest has been claimed before.

### Optional Quests

Optional quests can be rejected. Rejected quests are hidden from normal progression checks.

### Repeatable Quests

Repeatable quests can be completed, redeemed, and then restarted for another cycle.

### Hidden Under Dependency

When enabled, a quest stays hidden until all of its dependencies have been satisfied.

## Quest Book UI

Players can interact with Boundless through the quest book UI. Depending on config and screen mode, the interface can show:

- Category filters
- Pull-tab style filters or button-based filters
- Quest lists
- Category and sub-category grouping
- Quest detail panels
- Reward previews
- Dependency previews
- Settings access

The quest details panel shows:

- Quest name and icon
- Description
- Required dependencies
- Completion objectives and live progress
- Reward previews
- Completion, repeat, reject, pin, and quest scroll actions where applicable

## Quest Scrolls

Quest scroll support is optional and controlled by config. When enabled, a player can create a quest scroll for a quest they have already claimed, provided a scroll has not already been created for that quest.

This is useful for:

- Packaging progress milestones into portable items
- Handing quest markers to other players
- Custom pack-specific progression systems

## Filters And Display Modes

The mod supports multiple filter display modes:

- `tabs`: classic pull-tab filters
- `buttons`: legacy square filter buttons
- `hidden`: hides filters while leaving settings access available

This allows a pack author or player to choose the layout that best fits the rest of their UI.

## Built-In Quest Pack And External Packs

Boundless can load quest content from its built-in quest pack and from other enabled packs. Pack management in the editor allows enabling or disabling quest packs without removing their files.

This is useful for:

- Shipping a default quest pack with the mod
- Layering modpack-specific quest packs on top
- Turning packs on or off while testing
- Keeping example or legacy packs available but disabled

## In-Game Editor

The quest editor is intended to make pack maintenance easier without leaving the game. It supports:

- Creating new packs
- Browsing packs
- Enabling and disabling packs
- Editing categories
- Editing sub-categories
- Editing quests
- Grouping quests by category and sub-category
- Collapsing groups to reduce scrolling

The editor is aimed at pack creators who need fast iteration while testing progression.

## Config And Settings

The settings UI includes grouped config sections such as functionality, gameplay, and UI. Depending on your current build, settings may include options for:

- Built-in quest pack enable state
- Quest book availability
- Quest scroll support
- Quest pinning
- Auto-claim behavior
- Filter display mode
- Category visibility

These settings affect both player-facing behavior and pack-author workflows.

## Client And Server Behaviour

Boundless tracks progression on both sides:

- Server-side progress is authoritative in multiplayer
- Client-side cached progress is used for singleplayer and UI responsiveness
- Status updates, kill counts, and claim metadata are synced across the network

The mod is designed so malformed sync state or malformed identifiers should degrade safely instead of crashing the UI or blocking normal play.

## Authoring Quests

Quest content is defined through JSON resources. In practice, an author usually creates:

1. Categories
2. Sub-categories
3. Quests
4. Completion targets
5. Rewards

Recommended authoring approach:

1. Start with category layout first.
2. Add a few short, testable quests.
3. Verify dependencies and reward claims in-game.
4. Add sub-categories once a category becomes crowded.
5. Use repeatable and optional flags deliberately instead of everywhere.

## Tips For Pack Authors

- Keep quest IDs stable once a pack is in use.
- Prefer clear dependency chains over hidden implicit progression.
- Use sub-categories to keep large categories manageable.
- Treat submission quests carefully, especially for expensive items or XP costs.
- Validate reward item IDs and icons before shipping.
- Test both singleplayer and dedicated server flows.
- Check how the UI behaves at multiple GUI scales.

## Typical Player Flow

1. Open the quest book.
2. Browse a category or filter group.
3. Read the quest details.
4. Meet the objective requirements.
5. Claim the quest.
6. Receive item, loot, command, function, or XP rewards.
7. Repeat or reject quests when allowed.

## Compatibility Notes

- Boundless relies on valid Minecraft resource identifiers for item, entity, effect, advancement, and loot references.
- Invalid IDs should now fail safely in the UI and reward previews, but they still represent broken content that should be fixed in the quest data.
- Multiplayer quest progress depends on the server's quest data and sync state being current.

## Development Notes

This repository contains both gameplay code and editor/UI code. When changing the mod:

- Keep claim and submission logic conservative and failure-tolerant
- Treat network payload decoding as untrusted input
- Avoid client crashes from malformed resource IDs
- Prefer data validation and graceful fallback over hard failure in UI paths

## Summary

Boundless is intended to be a full questing toolkit for modpacks rather than a simple checklist overlay. It provides structured progression, rich rewards, editor tooling, and configurable UI behavior so both players and pack authors can shape the quest experience to fit the pack.
