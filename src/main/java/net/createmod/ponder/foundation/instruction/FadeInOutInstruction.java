package net.createmod.ponder.foundation.instruction;

import net.createmod.ponder.foundation.PonderScene;

public abstract class FadeInOutInstruction extends TickingInstruction {
    protected static final int FADE_TIME = 5;
    protected FadeInOutInstruction(int duration) { super(false, Math.max(0, duration) + FADE_TIME * 2); }
    protected abstract void show(PonderScene scene);
    protected abstract void hide(PonderScene scene);
    protected abstract void applyFade(PonderScene scene, float fade);
    @Override protected void firstTick(PonderScene scene) { show(scene); applyFade(scene, 0); }
    @Override protected void tickRunning(PonderScene scene, int elapsed, float progress) {
        float fade = elapsed < FADE_TIME ? elapsed / (float) FADE_TIME
            : remainingTicks < FADE_TIME ? remainingTicks / (float) FADE_TIME : 1;
        applyFade(scene, fade * fade);
    }
    @Override protected void finish(PonderScene scene) { applyFade(scene, 0); hide(scene); }
}
