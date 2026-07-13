import mods.ponder.SceneRegistry;
import mods.ponder.Selection;

val scene = SceneRegistry.create("minecraft:water_bucket", "ponder:water_handling",
    "Handling Water in a Scene", "ponder:demo/fluids");
scene.tag("ponder:rendering");
scene.configureBasePlate(0, 0, 5);
scene.showBasePlate();
scene.idle(20);
scene.world.showSection(Selection.layersFrom(1), "down");
scene.idle(20);

scene.overlay.showText(120, "The center block is a full water source, with flowing levels on each side.",
    2.5, 2.0, 2.5, "blue", true, false);
scene.addKeyframe();
scene.idle(150);

scene.world.setBlocks(Selection.fromTo(1, 1, 2, 3, 1, 2), "minecraft:air", true);
scene.effects.emitParticles("water_splash", 2.5, 1.5, 2.5, 0, 0.03, 0, 1.0, 20);
scene.overlay.showText(120, "Picking up the source explicitly clears the source and both flow blocks.",
    2.5, 1.5, 2.5, "blue", true, false);
scene.addKeyframe();
scene.idle(150);

scene.world.setBlock(2, 1, 2, "minecraft:water", true);
scene.world.setBlock(1, 1, 2, "minecraft:flowing_water[level=3]", true);
scene.world.setBlock(3, 1, 2, "minecraft:flowing_water[level=5]", true);
scene.overlay.showText(120, "Placing the bucket restores the source and two chosen flow levels.",
    2.5, 1.5, 2.5, "blue", true, false);
scene.addKeyframe();
scene.idle(150);

scene.overlay.showText(120, "Transparent edges keep the water surface visible while showing blocks behind it.",
    2.5, 2.0, 2.5, "green", true, false);
scene.effects.indicateSuccess(2, 1, 2);
scene.addKeyframe();
scene.idle(150);
scene.markAsFinished();
scene.register();
