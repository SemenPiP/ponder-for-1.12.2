package net.createmod.catnip.animation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PhysicalFloat {
    private float previousValue;
    private float value;
    private float previousSpeed;
    private float speed;
    private float limit = Float.NaN;
    private final float mass;
    private final List<Force> forces = new ArrayList<Force>();

    public static PhysicalFloat create() { return new PhysicalFloat(1); }
    public static PhysicalFloat create(float mass) { return new PhysicalFloat(mass); }
    public PhysicalFloat(float mass) {
        if (!(mass > 0) || Float.isInfinite(mass)) throw new IllegalArgumentException("mass must be finite and positive");
        this.mass = mass;
    }
    public PhysicalFloat startAt(double value) { previousValue = this.value = (float) value; return this; }
    public PhysicalFloat withDrag(double drag) { return addForce(new Force.Drag((float) drag)); }
    public PhysicalFloat zeroing(double acceleration) { return addForce(new Force.Zeroing((float) acceleration)); }
    public PhysicalFloat withLimit(float limit) {
        if (limit < 0) throw new IllegalArgumentException("limit cannot be negative");
        this.limit = limit;
        return this;
    }

    public void tick() {
        previousSpeed = speed;
        previousValue = value;
        float acceleration = 0;
        for (Force force : forces) acceleration += force.get(mass, value, speed) / mass;
        speed += acceleration;
        for (Iterator<Force> it = forces.iterator(); it.hasNext();) {
            if (it.next().finished()) it.remove();
        }
        if (!Float.isNaN(limit)) speed = Math.max(-limit, Math.min(limit, speed));
        value += speed;
    }

    public PhysicalFloat addForce(Force force) { if (force != null) forces.add(force); return this; }
    public PhysicalFloat bump(double force) { return addForce(new Force.Impulse((float) force)); }
    public PhysicalFloat bump(int time, double force) { return addForce(new Force.OverTime(time, (float) force)); }
    public float getValue() { return getValue(1); }
    public float getValue(float partialTicks) { return previousValue + (value - previousValue) * partialTicks; }
    public float getSpeed() { return speed; }
    public float getSpeed(float partialTicks) { return previousSpeed + (speed - previousSpeed) * partialTicks; }
}
