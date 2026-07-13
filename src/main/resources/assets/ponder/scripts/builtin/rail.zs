import mods.ponder.SceneRegistry;
import mods.ponder.Selection;

val scene = SceneRegistry.create("minecraft:rail", "ponder:rail_minecart",
    "Moving a Minecart along Rails", "ponder:demo/rail");
scene.tag("ponder:mechanics");
scene.configureBasePlate(0, 0, 5);
scene.showBasePlate();
scene.idle(20);
scene.world.showSection(Selection.layersFrom(1), "down");
scene.idle(20);

scene.overlay.showText(120, "This rail path travels south, turns at the curve, and then continues east.",
    2.0, 1.5, 2.5, "white", true, false);
scene.addKeyframe();
scene.idle(150);

scene.world.createMinecart("cart", 1.5, 1.1, 1.5, 0, "empty");
scene.world.moveMinecart("cart", 0, 0, 2, 80);
scene.overlay.showText(120, "A virtual minecart is created at the start and animated toward the corner.",
    1.5, 1.5, 3.0, "white", true, false);
scene.addKeyframe();
scene.idle(150);

scene.world.rotateMinecart("cart", -90, 55);
scene.world.moveMinecart("cart", 2, 0, 0, 85);
scene.rotateCameraY(-25);
scene.overlay.showText(120, "At the curve, the cart rotates and moves east along the final straight section.",
    3.0, 1.5, 3.5, "blue", true, false);
scene.addKeyframe();
scene.idle(150);

scene.effects.indicateSuccess(3, 1, 3);
scene.overlay.showText(120, "Animated elements remain deterministic, and replay creates the cart at its start.",
    3.0, 2.0, 3.0, "green", true, false);
scene.addKeyframe();
scene.idle(150);
scene.markAsFinished();
scene.register();
