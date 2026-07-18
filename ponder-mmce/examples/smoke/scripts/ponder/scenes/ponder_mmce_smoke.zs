import mods.ponder.SceneRegistry;
import mods.ponder.Selection;
import mods.ponder.mmce.MMCEStructures;

val fixed = MMCEStructures.machine("modularmachinery:ponder_mmce_static_demo");
val fixedScene = SceneRegistry.create(
    fixed.component,
    "ponder_mmce:static_demo",
    "MMCE Static Structure",
    fixed.structure
);
fixedScene.configureBasePlate(0, 0, fixed.basePlateSize);
fixedScene.showBasePlate();
fixedScene.idle(10);
fixedScene.world.showSection(Selection.structureGroup("mmce:all"), "down");
fixedScene.idle(20);
fixedScene.overlay.showText(60, "The structure is sampled directly from the loaded MMCE machine.",
    fixed.controllerX + 0.5, fixed.controllerY + 1.5, fixed.controllerZ + 0.5,
    "white", true, false);
fixedScene.world.hideSection(Selection.structureGroup("mmce:tag/item_input"), "up");
fixedScene.idle(10);
fixedScene.world.showSection(Selection.structureGroup("mmce:tag/item_input"), "down");
fixedScene.effects.indicateSuccess(fixed.controllerX, fixed.controllerY, fixed.controllerZ);
fixedScene.idle(30);
fixedScene.markAsFinished();
fixedScene.register();

val extended = MMCEStructures.dynamic(
    "modularmachinery:ponder_mmce_dynamic_demo",
    "line",
    3,
    "north",
    "north"
);
val dynamicScene = SceneRegistry.create(
    extended.component,
    "ponder_mmce:dynamic_demo",
    "MMCE Dynamic Pattern",
    extended.structure
);
dynamicScene.configureBasePlate(0, 0, extended.basePlateSize);
dynamicScene.showBasePlate();
dynamicScene.idle(10);
dynamicScene.world.showSection(Selection.structureGroup("mmce:controller"), "down");
dynamicScene.world.showSection(Selection.structureGroup("mmce:dynamic/line/segment/0/frame"), "down");
dynamicScene.idle(10);
dynamicScene.world.showSection(Selection.structureGroup("mmce:dynamic/line/segment/1/frame"), "down");
dynamicScene.idle(10);
dynamicScene.world.showSection(Selection.structureGroup("mmce:dynamic/line/segment/2/frame"), "down");
dynamicScene.idle(10);
dynamicScene.world.showSection(Selection.structureGroup("mmce:dynamic/line/end/output"), "down");
dynamicScene.overlay.showText(80, "Every repeated segment and the terminal output remain addressable.",
    extended.controllerX + 0.5, extended.controllerY + 1.5, extended.controllerZ + 2.5,
    "green", true, false);
dynamicScene.idle(40);
dynamicScene.markAsFinished();
dynamicScene.register();
