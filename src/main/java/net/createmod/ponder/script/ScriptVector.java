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
        return new ScriptVector(x, y, z);
    }
}
