# Operator Tools

Boundless exposes operator-facing tools for maintaining and editing quest content in-game.

## Access Level

Most admin features require permission level `2`.

## Quest Settings Screen

Use this for runtime behavior tuning without touching files directly.

## Quest Editor Screen

The editor supports:

- Creating quest packs
- Importing packs
- Editing pack options
- Creating/editing categories
- Creating/editing sub-categories
- Creating/editing quests
- Reordering entries
- Duplicating and deleting entries
- Exporting a pack zip

## Editor Workflow (Recommended)

1. Create or select a pack.
2. Define categories.
3. Define sub-categories.
4. Create quests and dependencies.
5. Test progression in normal gameplay.
6. Export backup zip before major changes.

## Safety Notes

- Treat production server packs like source code: back them up.
- Validate ids and dependencies before publishing.
- Reload after external file operations.
