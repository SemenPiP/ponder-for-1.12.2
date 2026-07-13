import mods.ponder.SceneRegistry;
import mods.ponder.Selection;

val scene = SceneRegistry.create("minecraft:piston", "ponder:piston_movement",
    "Moving Blocks with a Piston", "ponder:demo/piston");
scene.tag("ponder:mechanics");
scene.configureBasePlate(0, 0, 5);
scene.showBasePlate();
scene.idle(20);
scene.world.showSection(Selection.layersFrom(1), "down");
scene.idle(20);

scene.overlay.showText(120, "This piston faces east toward a slime block with one block of travel space.",
    1.5, 2.0, 2.5, "white", true, false);
scene.overlay.showControls(80, 1.5, 2.0, 2.5, "down", "right_click", "minecraft:redstone");
scene.addKeyframe();
scene.idle(150);

scene.world.showIndependentSection("moving_block", Selection.position(2, 1, 2), "east");
scene.world.moveSection("moving_block", 1, 0, 0, 65);
scene.effects.indicateRedstone(1, 1, 2);
scene.overlay.showText(120, "The slime block becomes an independent section and moves into the travel position.",
    3.0, 1.5, 2.5, "white", true, false);
scene.addKeyframe();
scene.idle(150);

scene.world.rotateSection("moving_block", 0, 90, 0, 60);
scene.overlay.showText(120, "Independent sections can also rotate while the anchor remains fixed.",
    4.0, 1.5, 2.5, "blue", true, false);
scene.addKeyframe();
scene.idle(150);

scene.world.rotateSection("moving_block", 0, -90, 0, 60);
scene.world.moveSection("moving_block", -1, 0, 0, 55);
scene.world.hideIndependentSection("moving_block", "up");
scene.overlay.showText(120, "The script retracts the piston and hides the moving section before replay.",
    2.5, 1.5, 2.5, "green", true, false);
scene.addKeyframe();
scene.idle(150);
scene.markAsFinished();
scene.register();
