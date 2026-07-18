import mods.ponder.SceneRegistry;
import mods.ponder.Selection;
import mods.ponder.SharedText;
import mods.ponder.TagRegistry;
import mods.ponder.Vector;

SharedText.register("ponder_example.pulse", "This step is provided by a versioned addon codec.");

val tag = TagRegistry.create("ponder_example:codec", "minecraft:redstone",
    "Codec Extension", "A server-synchronized custom instruction example.");
tag.addComponent("minecraft:paper");
tag.register();

val scene = SceneRegistry.create("minecraft:paper", "ponder_example:codec_sync",
    "Versioned Codec Synchronization", "ponder_example:codec_demo");
scene.tag("ponder_example:codec");
scene.configureBasePlate(0, 0, 5);
scene.showBasePlate();
scene.idle(10);
scene.world.showSection(Selection.layersFrom(1), "down");
scene.overlay.showSharedText(80, "ponder:ponder_example.pulse", [],
    Vector.of(2.5, 2.0, 2.5), "white", true, false);
scene.custom("ponder_example:pulse", {x: 2, y: 1, z: 2, duration: 40, color: "green"});
scene.addKeyframe();
scene.idle(30);
scene.markAsFinished();
scene.register();
