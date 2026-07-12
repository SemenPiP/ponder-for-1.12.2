package net.createmod.ponder.foundation.element;

import net.createmod.ponder.api.element.AnimatedOverlayElement;

public abstract class AnimatedOverlayElementBase extends PonderElementBase implements AnimatedOverlayElement {
    protected float previousFade = 1;
    protected float fade = 1;
    @Override public void setFade(float value) { previousFade=fade;fade=Math.max(0,Math.min(1,value));visible=fade>0||previousFade>0; }
    @Override public float getFade(float partialTicks) { return previousFade+(fade-previousFade)*partialTicks; }
    @Override public Object captureState() { return new State(visible, previousFade, fade); }
    @Override public void restoreState(Object value) {
        if (!(value instanceof State)) { super.restoreState(value); return; }
        State state=(State)value;visible=state.visible;previousFade=state.previous;fade=state.fade;
    }
    private static final class State { final boolean visible;final float previous,fade;State(boolean v,float p,float f){visible=v;previous=p;fade=f;} }
}
