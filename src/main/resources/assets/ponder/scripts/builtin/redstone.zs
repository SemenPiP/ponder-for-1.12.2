import mods.ponder.SceneRegistry;
import mods.ponder.Selection;
import mods.ponder.TagRegistry;

val tag = TagRegistry.create("ponder:redstone", "minecraft:redstone_lamp", "Redstone",
    "See powered states and signals change step by step.");
tag.addComponent("minecraft:redstone_lamp");
tag.register();

val scene = SceneRegistry.create("minecraft:redstone_lamp", "ponder:redstone_lamp_power",
    "Powering a Redstone Lamp", "ponder:demo/redstone");
scene.tag("ponder:redstone");
scene.configureBasePlate(0, 0, 5);
scene.showBasePlate();
scene.idle(20);
scene.world.showSection(Selection.layersFrom(1), "down");
scene.idle(20);

scene.overlay.showText(120, "A redstone block supplies power to the wire leading toward the lamp.",
    1.5, 2.0, 2.5, "white", true, false);
scene.addKeyframe();
scene.idle(150);

scene.world.toggleRedstonePower(Selection.fromTo(2, 1, 2, 3, 1, 2));
scene.effects.indicateRedstone(2, 1, 2);
scene.effects.indicateRedstone(3, 1, 2);
scene.overlay.showText(120, "The scene sets the wire signal to powered and marks the path with particles.",
    2.5, 1.5, 2.5, "red", true, false);
scene.addKeyframe();
scene.idle(150);

scene.world.setBlock(4, 1, 2, "minecraft:lit_redstone_lamp", true);
scene.effects.indicateSuccess(4, 1, 2);
scene.overlay.showText(120, "The lamp is replaced with its lit state without requiring neighbor simulation.",
    4.0, 1.5, 2.5, "red", true, false);
scene.addKeyframe();
scene.idle(150);

scene.world.toggleRedstonePower(Selection.fromTo(2, 1, 2, 3, 1, 2));
scene.world.setBlock(4, 1, 2, "minecraft:redstone_lamp", false);
scene.overlay.showText(120, "Finally, the wire and lamp return to their unpowered states.",
    3.5, 1.5, 2.5, "green", true, false);
scene.addKeyframe();
scene.idle(150);
scene.markAsFinished();
scene.register();
