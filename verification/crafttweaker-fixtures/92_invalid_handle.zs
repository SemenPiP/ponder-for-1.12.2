import mods.ponder.SceneRegistry;

val invalidHandle = SceneRegistry.create("minecraft:paper", "ponder_fixture:invalid_handle",
    "Invalid Handle Fixture", "ponder:demo/basics");
invalidHandle.world.moveItem("missing_item", 1, 0, 0, 10);
invalidHandle.register();
