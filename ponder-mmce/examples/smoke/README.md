# Ponder-MMCE Smoke Pack

This directory is a directly installable Minecraft 1.12.2 test pack fragment.
Copy `config` and `scripts` into an instance that contains:

- Forge 14.23.5.2847 or newer
- MixinBooter 11.2
- CraftTweaker 4.1.20.698
- MMCE 2.3.2
- Ponder Legacy 1.1.2
- Ponder-MMCE 0.1.0-alpha

The pack defines three MMCE machines:

- `modularmachinery:ponder_mmce_static_demo` has an authored static Ponder scene.
- `modularmachinery:ponder_mmce_dynamic_demo` has an authored three-segment dynamic scene.
- `modularmachinery:ponder_mmce_unconfigured` intentionally has no Ponder scene.

Use an MMCE blueprint associated with each machine to verify the normal hover
and `W` entry. The third blueprint must show the localized no-scene message.
The example does not generate a fallback scene.
