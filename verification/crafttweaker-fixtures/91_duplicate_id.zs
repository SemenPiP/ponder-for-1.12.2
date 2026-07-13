import mods.ponder.SceneRegistry;

val duplicate = SceneRegistry.create("minecraft:paper", "ponder_fixture:advanced",
    "Duplicate Fixture", "ponder:demo/basics");
duplicate.idle(1);
duplicate.register();
