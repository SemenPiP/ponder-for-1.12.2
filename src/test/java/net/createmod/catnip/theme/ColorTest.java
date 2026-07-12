package net.createmod.catnip.theme;

import static org.junit.Assert.*;

import org.junit.Test;

public class ColorTest {
    @Test public void immutableConstantsReturnMutableCopyOnMutation(){
        Color changed=Color.WHITE.setAlpha(64);assertNotSame(Color.WHITE,changed);assertEquals(255,Color.WHITE.getAlpha());assertEquals(64,changed.getAlpha());
    }
    @Test public void argbMixIncludesAlpha(){assertEquals(0x7F7F7F7F,Color.mixColors(0x00000000,0xFFFFFFFF,.5f));}
    @Test public void rainbowHandlesIntegerMinimum(){assertNotNull(Color.rainbowColor(Integer.MIN_VALUE));}
}
