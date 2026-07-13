import mods.ponder.SceneRegistry;
import mods.ponder.Selection;
import mods.ponder.TagRegistry;

val tag = TagRegistry.create("ponder:rendering", "minecraft:glass", "Rendering",
    "Compare render layers and transparent fluids in the virtual world.");
tag.addComponent("minecraft:glass");
tag.addComponent("minecraft:water_bucket");
tag.register();

val scene = SceneRegistry.create("minecraft:glass", "ponder:glass_render_layers",
    "Understanding Render Layers", "ponder:demo/render_layers");
scene.tag("ponder:rendering");
scene.configureBasePlate(0, 0, 5);
scene.showBasePlate();
scene.idle(20);
scene.world.showSection(Selection.layersFrom(1), "down");
scene.idle(20);

scene.overlay.showText(120, "Solid blocks write every visible pixel in the solid render layer.",
    1.5, 1.5, 1.5, "white", true, false);
scene.addKeyframe();
scene.idle(150);

scene.overlay.showText(120, "Cutout textures discard transparent pixels to produce sharp-edged details.",
    2.5, 1.5, 1.5, "green", true, false);
scene.addKeyframe();
scene.idle(150);

scene.overlay.showText(120, "Mipmap cutout textures stay filtered at distance while keeping transparent gaps.",
    3.5, 1.5, 1.5, "blue", true, false);
scene.addKeyframe();
scene.idle(150);

scene.world.showIndependentSection("glass", Selection.position(4, 1, 2), "down");
scene.world.moveSection("glass", 0, 0.5, 0, 40);
scene.world.moveSection("glass", 0, -0.5, 0, 40);
scene.overlay.showText(120, "Stained glass uses the translucent layer and blends with geometry behind it.",
    4.0, 1.5, 2.5, "blue", true, false);
scene.addKeyframe();
scene.idle(150);
scene.markAsFinished();
scene.register();
