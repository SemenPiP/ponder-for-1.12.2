# Ponder ZenScript example pack

Copy the contents of this directory into the Minecraft client directory. The
resulting layout must be:

```text
<game>/
`-- scripts/
   `-- ponder/
      |-- scenes/
      |  `-- ponder_zen_diagnostics.zs
      `-- structures/
         `-- ponder/
            `-- demo/
               `-- basics.nbt
```

The active scene uses the external `ponder:demo/basics` structure. The NBT is
copied from Ponder's real demo structure so the example can be installed
without relying on the mod jar resource fallback.

The files ending in `.zs.disabled` are intentionally inactive. Rename one to
`.zs` only when testing that diagnostic case:

- `90_duplicate_id.zs.disabled`: duplicates the active scene ID.
- `91_missing_structure.zs.disabled`: references an NBT file that is not
  present.
- `92_invalid_handle.zs.disabled`: uses a handle containing a space.
- `93_oversized_nbt.zs.disabled`: sends Tile NBT over the 256 KiB text safety
  limit.

## Diagnostic commands

Run these commands in the client or server command console after restarting
Minecraft. ZenScript source changes require a restart. `/ponder reload` only
reapplies already compiled definitions and refreshes structure caches.

```text
/ponder list effective
/ponder inspect ponder_zen:diagnostics_demo effective
/ponder validate effective
/ponder export ponder_zen:diagnostics_demo all effective
/ponder sync status
```

`list` shows the effective scene set and its source, component, and structure.
`inspect` prints the scene source, structure provider and fingerprint,
instruction count, timeline totals, and issues. `validate` runs the structural
validation task and writes the validation report under
`logs/ponder/diagnostics`. `export` writes the script IR and timeline report
there; use `ir` or `timeline` instead of `all` for one report. `sync status`
shows active snapshot transfers; it requires the normal operator permission on
the server.

The active scene ID is `ponder_zen:diagnostics_demo`. Open it directly with:

```text
/ponder ponder_zen:diagnostics_demo
```
