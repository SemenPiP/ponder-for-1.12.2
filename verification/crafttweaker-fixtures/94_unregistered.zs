import mods.ponder.SceneRegistry;

val unregistered = SceneRegistry.create("minecraft:paper", "ponder_fixture:unregistered",
    "Unregistered Fixture", "ponder:demo/basics");
unregistered.idle(1);
