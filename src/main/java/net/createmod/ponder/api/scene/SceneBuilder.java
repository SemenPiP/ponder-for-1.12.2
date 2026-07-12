package net.createmod.ponder.api.scene;

import java.util.function.Consumer;

import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.instruction.PonderInstruction;

public interface SceneBuilder {
    OverlayInstructions overlay();
    WorldInstructions world();
    DebugInstructions debug();
    EffectInstructions effects();
    SpecialInstructions special();
    PonderScene getScene();
    void title(String sceneId, String title);
    void configureBasePlate(int xOffset, int zOffset, int basePlateSize);
    void scaleSceneView(float factor);
    void removeShadow();
    void setSceneOffsetY(float yOffset);
    void showBasePlate();
    void addInstruction(PonderInstruction instruction);
    void addInstruction(Consumer<PonderScene> callback);
    void idle(int ticks);
    void idleSeconds(int seconds);
    void markAsFinished();
    void setNextUpEnabled(boolean enabled);
    void rotateCameraY(float degrees);
    void addKeyframe();
    void addLazyKeyframe();
}
