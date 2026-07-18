import mods.ponder.Position;
import mods.ponder.SceneRegistry;
import mods.ponder.Selection;
import mods.ponder.SharedText;
import mods.ponder.TagRegistry;
import mods.ponder.Vector;

SharedText.register("ponder_zen.step", "Step %s: %s");

val tag = TagRegistry.create("ponder_zen:diagnostics", "minecraft:crafting_table",
    "ZenScript Diagnostics", "A complete external-structure Ponder example.");
tag.addComponent("minecraft:crafting_table");
tag.register();

val scene = SceneRegistry.create("minecraft:crafting_table", "ponder_zen:diagnostics_demo",
    "Ponder ZenScript Diagnostics", "ponder:demo/basics");
scene.tag("ponder_zen:diagnostics");
scene.configureBasePlate(0, 0, 5);
scene.showBasePlate();
scene.idle(10);

scene.world.showSection(Selection.layersFrom(1), "down");
scene.idle(20);
scene.movePointOfInterest(Vector.of(2.5, 1.5, 2.5));
scene.overlay.showSharedText(100, "ponder:ponder_zen.step", ["1", "Reveal the external NBT structure"],
    Vector.of(2.5, 2.0, 2.5), "white", true, false);
scene.overlay.showOutlineWithText("The loaded demo structure", "blue",
    Selection.fromTo(0, 1, 0, 4, 2, 4), 80, false);
scene.addKeyframe();
scene.idle(100);

scene.world.modifyTileNBT(Selection.position(3, 1, 2), {Lock: "ponder_zen_demo"}, false, false);
scene.overlay.showControls(80, Vector.of(3.5, 1.5, 2.5), "down", "right_click", "minecraft:chest");
scene.overlay.showBoundingBox("green", "chest_bounds",
    Vector.of(2, 1, 2), Vector.of(4, 2, 3), 80);
scene.idle(90);

scene.world.makeSectionIndependent("chest_section", Selection.position(3, 1, 2));
scene.world.moveSection("chest_section", Vector.of(0, 1, 0), 20);
scene.world.rotateSection("chest_section", Vector.of(0, 90, 0), 20);
scene.overlay.showSharedText(100, "ponder:ponder_zen.step", ["2", "Move a typed section handle"],
    Vector.of(3.5, 2.5, 2.5), "blue", true, false);
scene.overlay.showLine("red", Vector.of(2.5, 1.0, 2.5), Vector.of(3.5, 2.0, 2.5), 60, true);
scene.addKeyframe();
scene.idle(110);

scene.world.createItemEntity("book_item", Vector.of(2.5, 2.2, 2.5), Vector.of(0, 0.02, 0),
    "minecraft:book", 1, 0);
scene.world.moveItem("book_item", Vector.of(0, 0.5, 0), 20);
scene.overlay.showSharedText(100, "ponder:ponder_zen.step", ["3", "Create and move an item entity handle"],
    Vector.of(2.5, 2.0, 2.5), "green", true, false);
scene.overlay.showScrollInput(Vector.of(3.5, 2.0, 2.5), "up", 60);
scene.effects.indicateSuccess(Position.of(3, 1, 2));
scene.addKeyframe();
scene.idle(100);

scene.world.hideItem("book_item");
scene.idle(20);
scene.world.showItem("book_item");
scene.world.removeItem("book_item");
scene.world.hideIndependentSection("chest_section", "up");
scene.idle(20);
scene.markAsFinished();
scene.register();
