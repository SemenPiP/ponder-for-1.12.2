import mods.ponder.Position;
import mods.ponder.SceneRegistry;
import mods.ponder.Selection;
import mods.ponder.SharedText;
import mods.ponder.TagRegistry;
import mods.ponder.Vector;

function fixtureDelay() as int {
    return 5;
}

SharedText.register("fixture.summary", "Fixture values: %s / %s");

val tag = TagRegistry.create("ponder_fixture:advanced", "minecraft:paper", "Advanced Fixture",
    "Exercises deterministic ZenScript scene APIs in the real CraftTweaker runtime.");
tag.addComponent("minecraft:paper");
tag.register();

val scene = SceneRegistry.create("minecraft:paper", "ponder_fixture:advanced",
    "Advanced CraftTweaker Fixture", "ponder:demo/basics");
scene.tag("ponder_fixture:advanced");
scene.configureBasePlate(0, 0, 5);
scene.showBasePlate();

for i in 0 to 2 {
    scene.idle(fixtureDelay());
}

if (true) {
    scene.world.showSection(Selection.layers(1, 2), "down");
    scene.world.setBlock(Position.of(1, 1, 1), "minecraft:stone", false);
    scene.effects.indicateSuccess(Position.of(1, 1, 1));
    scene.effects.movePointOfInterest(Vector.of(1.5, 1.5, 1.5));
}

scene.overlay.showSharedText(20, "ponder:fixture.summary", ["left", "right"],
    Vector.of(1.5, 2.0, 1.5), "white", true, false);
scene.overlay.showIndependentText(20, "Independent fixture text", 18, "blue", false);
scene.overlay.showBoundingBox("green", "fixture_bounds", Vector.of(0, 0, 0), Vector.of(2, 2, 2), 20);
scene.overlay.showScrollInput(Vector.of(1.5, 1.5, 1.5), "up", 10);
scene.overlay.showCenteredScrollInput(Position.of(1, 1, 1), "north", 10);
scene.overlay.showRepeaterScrollInput(Position.of(1, 1, 1), 10);
scene.overlay.showFilterSlotInput(Vector.of(1.5, 1.5, 1.5), 10);

scene.world.createItemEntity("fixture_item", Vector.of(1.5, 2.0, 1.5), Vector.of(0, 0, 0),
    "minecraft:paper", 1, 0);
scene.world.moveItem("fixture_item", Vector.of(1, 0, 0), 10);
scene.world.hideItem("fixture_item");
scene.world.showItem("fixture_item");
scene.world.removeItem("fixture_item");
scene.world.modifyTileNBT(Selection.position(3, 1, 2), {Lock: "ponder_fixture"}, false, false);
scene.idle(10);
scene.markAsFinished();
scene.register();
