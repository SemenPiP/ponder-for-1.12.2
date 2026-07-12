package net.createmod.ponder.foundation.instruction;

import net.createmod.ponder.foundation.PonderScene;

public abstract class TickingInstruction extends PonderInstruction {
    private final boolean blocking;
    protected final int totalTicks;
    protected int remainingTicks;
    private boolean began;

    protected TickingInstruction(boolean blocking, int ticks) {
        if (ticks < 0)
            throw new IllegalArgumentException("Instruction duration may not be negative");
        this.blocking = blocking;
        this.totalTicks = ticks;
        this.remainingTicks = ticks;
    }

    protected void firstTick(PonderScene scene) {
    }

    protected void tickRunning(PonderScene scene, int elapsed, float progress) {
    }

    protected void finish(PonderScene scene) {
    }

    @Override
    public void reset(PonderScene scene) {
        remainingTicks = totalTicks;
        began = false;
    }

    @Override
    public final void tick(PonderScene scene) {
        if (!began) {
            began = true;
            firstTick(scene);
            if (totalTicks == 0) {
                tickRunning(scene, 0, 1);
                finish(scene);
                return;
            }
        }
        if (remainingTicks <= 0)
            return;
        int elapsed = totalTicks - remainingTicks + 1;
        remainingTicks--;
        float progress = totalTicks == 0 ? 1 : elapsed / (float) totalTicks;
        tickRunning(scene, elapsed, Math.max(0, Math.min(1, progress)));
        if (remainingTicks == 0)
            finish(scene);
    }

    @Override public final boolean isComplete() { return began && remainingTicks == 0; }
    @Override public final boolean isBlocking() { return blocking; }
    @Override public final int getDuration() { return totalTicks; }

    @Override public Object captureState() { return new State(remainingTicks, began); }
    @Override public void restoreState(Object value) {
        if (value instanceof State) {
            State state = (State) value;
            remainingTicks = state.remaining;
            began = state.began;
        }
    }

    private static final class State {
        final int remaining;
        final boolean began;
        State(int remaining, boolean began) { this.remaining = remaining; this.began = began; }
    }
}
