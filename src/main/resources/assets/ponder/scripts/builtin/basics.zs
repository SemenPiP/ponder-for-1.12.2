import mods.ponder.SceneRegistry;
import mods.ponder.Selection;
import mods.ponder.TagRegistry;

val tag = TagRegistry.create("ponder:basics", "minecraft:crafting_table", "Ponder Basics",
    "Learn scene controls and the building blocks used by Ponder tutorials.");
tag.addComponent("minecraft:crafting_table");
tag.register();

val scene = SceneRegistry.create("minecraft:crafting_table", "ponder:ponder_basics",
    "Ponder for Minecraft 1.12.2", "ponder:demo/basics");
scene.tag("ponder:basics");
scene.configureBasePlate(0, 0, 5);
scene.showBasePlate();
scene.idle(20);
scene.world.showSection(Selection.layersFrom(1), "down");
scene.idle(20);

scene.overlay.showText(120, "Ponder scenes reveal a prepared structure one layer at a time.",
    2.5, 1.0, 2.5, "white", true, false);
scene.addKeyframe();
scene.idle(150);

scene.world.showIndependentSection("chest", Selection.position(3, 1, 2), "down");
scene.world.moveSection("chest", 0, 1, 0, 55);
scene.world.rotateSection("chest", 0, 90, 0, 55);
scene.overlay.showText(120, "Independent sections can move and rotate without changing the source structure.",
    3.5, 2.0, 2.5, "white", true, false);
scene.addKeyframe();
scene.idle(150);

scene.world.createItemEntity("book", 2.5, 2.2, 2.5, 0, 0, 0, "minecraft:book", 1, 0);
scene.effects.indicateSuccess(2, 1, 2);
scene.rotateCameraY(35);
scene.overlay.showText(120, "Overlays, particles and virtual entities call attention to each explanation step.",
    2.5, 2.0, 2.5, "green", true, false);
scene.addKeyframe();
scene.idle(150);

scene.world.moveSection("chest", 0, -1, 0, 55);
scene.world.rotateSection("chest", 0, -90, 0, 55);
scene.overlay.showText(120, "Keyframes make replaying and seeking deterministic.",
    3.0, 1.5, 2.5, "green", true, false);
scene.addKeyframe();
scene.idle(150);
scene.markAsFinished();
scene.register();
