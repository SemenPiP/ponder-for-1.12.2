import mods.ponder.SceneRegistry;
import mods.ponder.Selection;
import mods.ponder.TagRegistry;

val tag = TagRegistry.create("ponder:mechanics", "minecraft:piston", "Mechanics",
    "Follow explicit movement, processing and transport sequences.");
tag.addComponent("minecraft:furnace");
tag.addComponent("minecraft:piston");
tag.addComponent("minecraft:rail");
tag.register();

val scene = SceneRegistry.create("minecraft:furnace", "ponder:furnace_smelting",
    "Smelting with a Furnace", "ponder:demo/smelting");
scene.tag("ponder:mechanics");
scene.configureBasePlate(0, 0, 5);
scene.showBasePlate();
scene.idle(20);
scene.world.showSection(Selection.layersFrom(1), "down");
scene.idle(20);

scene.world.createItemEntity("ore", 1.5, 2.0, 2.5, 0, 0, 0, "minecraft:iron_ore", 1, 0);
scene.world.createItemEntity("fuel", 3.5, 2.0, 2.5, 0, 0, 0, "minecraft:coal", 1, 0);
scene.overlay.showText(120, "A furnace combines an input item with fuel to produce a smelted output.",
    2.5, 2.0, 2.5, "white", true, false);
scene.addKeyframe();
scene.idle(150);

scene.world.setBlock(2, 1, 2, "minecraft:lit_furnace[facing=north]", true);
scene.effects.emitParticles("flame", 2.5, 2.0, 2.5, 0, 0.03, 0, 1.0, 20);
scene.overlay.showText(120, "The demonstration explicitly lights the furnace and fills its input and fuel slots.",
    2.5, 2.0, 2.5, "output", true, false);
scene.addKeyframe();
scene.idle(150);

scene.effects.emitParticles("smoke_normal", 2.5, 2.0, 2.5, 0, 0.03, 0, 1.0, 20);
scene.overlay.showText(120, "Cook progress is scripted here for a clear and deterministic timeline.",
    2.5, 2.0, 2.5, "white", true, false);
scene.addKeyframe();
scene.idle(150);

scene.world.setBlock(2, 1, 2, "minecraft:furnace[facing=north]", false);
scene.world.createItemEntity("output", 2.5, 2.2, 2.5, 0, 0.04, 0, "minecraft:iron_ingot", 1, 0);
scene.effects.indicateSuccess(2, 1, 2);
scene.overlay.showText(120, "When scripted progress completes, the iron ingot appears as the output.",
    2.5, 1.5, 2.5, "green", true, false);
scene.addKeyframe();
scene.idle(150);
scene.markAsFinished();
scene.register();
