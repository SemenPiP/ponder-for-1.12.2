package net.createmod.ponder.script;

import crafttweaker.annotations.ZenRegister;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;
import stanhebben.zenscript.annotations.ZenProperty;

@ZenRegister
@ZenClass("mods.ponder.Position")
public final class ScriptPosition {
    @ZenProperty public final int x;
    @ZenProperty public final int y;
    @ZenProperty public final int z;

    private ScriptPosition(int x, int y, int z) {
        this.x = x; this.y = y; this.z = z;
    }

    @ZenMethod
    public static ScriptPosition of(int x, int y, int z) {
        return new ScriptPosition(x, y, z);
    }
}
