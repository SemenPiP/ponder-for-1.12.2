package net.createmod.ponder.script;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ScriptSourceMetadataTest {
    @Test
    public void normalizesScriptPathsAndPreservesLineNumber() {
        assertEquals("scripts/ponder/scenes/demo.zs:42",
            ScriptSourceMetadata.normalize("C:\\pack\\scripts\\ponder\\scenes\\demo.zs:42"));
        assertTrue(ScriptSourceMetadata.isBuiltin("scripts/ponder/builtin/basics.zs:3"));
        assertFalse(ScriptSourceMetadata.isBuiltin("scripts/ponder/scenes/basics.zs:3"));
    }

    @Test
    public void absolutePathsOutsideScriptsAreNotExposed() {
        String normalized = ScriptSourceMetadata.normalize("C:\\private\\author\\scene.zs:7");
        assertEquals("scripts/ponder/unknown:7", normalized);
        assertFalse(normalized.contains("private"));
    }
}
