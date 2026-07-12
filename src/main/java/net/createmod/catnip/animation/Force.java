package net.createmod.catnip.animation;

public interface Force {
    float get(float mass, float value, float speed);
    boolean finished();

    final class Drag implements Force {
        private final float dragFactor;
        public Drag(float dragFactor) { this.dragFactor = dragFactor; }
        public float get(float mass, float value, float speed) { return -speed * dragFactor; }
        public boolean finished() { return false; }
    }

    final class Zeroing implements Force {
        private final float accelerationPerTick;
        public Zeroing(float accelerationPerSecond) { this.accelerationPerTick = accelerationPerSecond / 20f; }
        public float get(float mass, float value, float speed) {
            return -Math.signum(value) * accelerationPerTick * mass;
        }
        public boolean finished() { return false; }
    }

    final class Impulse implements Force {
        private final float force;
        public Impulse(float force) { this.force = force; }
        public float get(float mass, float value, float speed) { return force; }
        public boolean finished() { return true; }
    }

    final class OverTime implements Force {
        private int timeRemaining;
        private final float forcePerTick;
        public OverTime(int time, float totalForce) {
            if (time <= 0) throw new IllegalArgumentException("time must be positive");
            this.timeRemaining = time;
            this.forcePerTick = totalForce / time;
        }
        public float get(float mass, float value, float speed) {
            timeRemaining--;
            return forcePerTick;
        }
        public boolean finished() { return timeRemaining <= 0; }
    }

    final class Static implements Force {
        private final float force;
        public Static(float force) { this.force = force; }
        public float get(float mass, float value, float speed) { return force; }
        public boolean finished() { return false; }
    }
}
