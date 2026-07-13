package net.createmod.ponder.script;

import crafttweaker.annotations.ZenRegister;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;
import stanhebben.zenscript.annotations.ZenProperty;

@ZenRegister
@ZenClass("mods.ponder.Vector")
public final class ScriptVector {
    @ZenProperty public final double x;
    @ZenProperty public final double y;
    @ZenProperty public final double z;

    private ScriptVector(double x, double y, double z) {
        this.x = x; this.y = y; this.z = z;
    }

    @ZenMethod
    public static ScriptVector of(double x, double y, double z) {
        validate(x, y, z, "Vector");
        return new ScriptVector(x, y, z);
    }

    static ScriptVector require(ScriptVector vector, String label) {
        if (vector == null) throw new IllegalArgumentException(label + " is required");
        validate(vector.x, vector.y, vector.z, label);
        return vector;
    }

    static void validate(double x, double y, double z, String label) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z))
            throw new IllegalArgumentException(label + " components must be finite");
    }
}
