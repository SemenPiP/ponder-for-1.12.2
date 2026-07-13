import mods.ponder.SceneRegistry;
import mods.ponder.Selection;

val oversized = SceneRegistry.create("minecraft:paper", "ponder_fixture:oversized_nbt",
    "Oversized NBT Fixture", "ponder:demo/basics");
var payload = "";
for i in 0 to 17000 {
    payload += "0123456789abcdef";
}
oversized.world.modifyTileNBT(Selection.position(2, 1, 2), {payload: payload}, false, false);
oversized.register();
