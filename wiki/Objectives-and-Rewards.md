# Objectives and Rewards

## Objective Types

Boundless supports these completion target types:

- `item` (collect/hold)
- `submit` (turn in items)
- `entity` (kill target entities)
- `effect` (have a potion/effect active)
- `advancement` (complete an advancement)
- `stat` (reach a stat threshold)
- `xp` (reach xp or level thresholds)
- `levelup_level` (LevelUP integration)
- `field` (text input objective)

## How Progress Is Counted

Progress is tracked by objective type and synced from server state in multiplayer.

- Collection/submit checks item id and quantity.
- Entity/effect/advancement/stat checks use exact ids.
- XP and level objectives use numeric thresholds.
- Field objectives require matching expected input criteria.

## Rewards

Quest rewards can include:

- Item rewards
- Command rewards
- Function rewards
- Loot table rewards
- XP/levels/LevelUP XP rewards

## Reward Claim Behavior

- Some quests can be auto-completed/auto-claimed if configured.
- Others require manual claim from the quest book UI.

## Pack Authoring Tip (Player/Admin Context)

If a quest seems to never complete, the most common cause is target id mismatch (item/entity/effect/advancement/stat key).
