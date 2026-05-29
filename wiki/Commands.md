# Commands

Root command:

`/boundless`

Most command actions require operator permissions.

## Common Commands

- `/boundless reload`
- `/boundless reset ...`
- `/boundless complete ...`
- `/boundless redeem ...`
- `/boundless questpack list`
- `/boundless questpack enable <pack>`
- `/boundless questpack disable <pack>`
- `/boundless toasts enable`
- `/boundless toasts disable`
- `/boundless toasts status`

## When to Use `reload`

Run `/boundless reload` after:

- Manual JSON edits
- Adding/removing pack files
- Enabling/disabling packs from outside the editor flow

## Operational Advice

- Prefer targeted commands over global reset/complete unless you explicitly want broad changes.
- On servers, communicate reload windows to avoid confusion while quest state refreshes.
