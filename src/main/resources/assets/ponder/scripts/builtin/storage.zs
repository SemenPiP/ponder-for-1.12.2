import mods.ponder.SceneRegistry;
import mods.ponder.Selection;
import mods.ponder.TagRegistry;

val tag = TagRegistry.create("ponder:storage", "minecraft:chest", "Storage",
    "Inspect inventories, item entities and block entity data.");
tag.addComponent("minecraft:chest");
tag.register();

val scene = SceneRegistry.create("minecraft:chest", "ponder:chest_storage",
    "Storing Items in a Chest", "ponder:demo/storage");
scene.tag("ponder:storage");
scene.configureBasePlate(0, 0, 5);
scene.showBasePlate();
scene.idle(20);
scene.world.showSection(Selection.layersFrom(1), "down");
scene.idle(20);
scene.world.showIndependentSection("chest", Selection.position(2, 1, 2), "down");

scene.overlay.showText(120, "Right-click a chest in the world to open its 27 inventory slots.",
    2.5, 2.0, 2.5, "white", true, false);
scene.overlay.showControls(80, 2.5, 2.0, 2.5, "down", "right_click", "minecraft:chest");
scene.addKeyframe();
scene.idle(150);

scene.world.createItemEntity("stored_item", 2.5, 2.2, 2.5, 0, 0, 0, "minecraft:book", 1, 0);
scene.overlay.showText(120, "Scene scripts can display matching item entities above the block.",
    2.5, 2.0, 2.5, "white", true, false);
scene.addKeyframe();
scene.idle(150);

scene.world.rotateSection("chest", 0, 90, 0, 40);
scene.effects.emitParticles("enchantment_table", 2.5, 2.0, 2.5, 0, 0.03, 0, 1.0, 20);
scene.overlay.showText(120, "Block entity presentation can be inspected while the section is transformed.",
    2.5, 2.0, 2.5, "blue", true, false);
scene.addKeyframe();
scene.idle(150);

scene.world.rotateSection("chest", 0, -90, 0, 40);
scene.effects.indicateSuccess(2, 1, 2);
scene.overlay.showText(120, "Inventory state and section transforms are restored when the scene replays.",
    2.5, 1.5, 2.5, "green", true, false);
scene.addKeyframe();
scene.idle(150);
scene.markAsFinished();
scene.register();
