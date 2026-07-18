package net.createmod.ponder.api.script;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Test;

import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.script.ScriptCodecDescriptors;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

public class ScriptInstructionCodecDescriptorTest {
    @Test
    public void descriptorSortsCapabilitiesAndChecksRequirements() {
        ScriptInstructionCodecDescriptor supported = new ScriptInstructionCodecDescriptor(
            id("codec"), 2, Arrays.asList(id("z"), id("a")));
        ScriptInstructionCodecDescriptor requirement = new ScriptInstructionCodecDescriptor(
            id("codec"), 2, Collections.singleton(id("a")));

        assertEquals(Arrays.asList(id("a"), id("z")),
            Arrays.asList(supported.getCapabilities().toArray(new ResourceLocation[0])));
        assertTrue(supported.satisfies(requirement));
        assertFalse(requirement.satisfies(supported));
        assertFalse(supported.satisfies(new ScriptInstructionCodecDescriptor(
            id("codec"), 3, Collections.singleton(id("a")))));
    }

    @Test
    public void registrationRejectsCapabilitiesRequiredButNotAdvertised() {
        ScriptInstructionCodec codec = new ScriptInstructionCodec() {
            @Override public ResourceLocation getId() { return id("invalid_requirement"); }
            @Override public Set<ResourceLocation> getRequiredCapabilities(NBTTagCompound data) {
                return Collections.singleton(id("missing"));
            }
            @Override public void validate(NBTTagCompound data) {
            }
            @Override public void program(NBTTagCompound data, SceneBuilder scene, SceneBuildingUtil util) {
            }
        };
        ScriptInstructionCodecs.register(codec);
        try {
            net.createmod.ponder.script.ScriptCodecDescriptors.requirement(codec, new NBTTagCompound());
            fail("Unadvertised required capability was accepted");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("does not advertise"));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void duplicateCapabilitiesAreRejected() {
        new ScriptInstructionCodecDescriptor(id("codec"), 1,
            Arrays.asList(id("same"), id("same")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void nonPositiveProtocolVersionIsRejected() {
        new ScriptInstructionCodecDescriptor(id("codec"), 0, new LinkedHashSet<ResourceLocation>());
    }

    @Test
    public void compatibilityRequiresCodecVersionAndCapabilityBeforeTransfer() {
        ScriptInstructionCodecDescriptor required =
            new ScriptInstructionCodecDescriptor(id("sync"), 2, Collections.singleton(id("outline")));
        assertTrue(ScriptCodecDescriptors.compatibilityError(
            Collections.<ScriptInstructionCodecDescriptor>emptyList(),
            Collections.singletonList(required)).contains("Missing required"));
        assertTrue(ScriptCodecDescriptors.compatibilityError(
            Collections.singletonList(new ScriptInstructionCodecDescriptor(
                id("sync"), 1, Collections.singleton(id("outline")))),
            Collections.singletonList(required)).contains("version mismatch"));
        assertTrue(ScriptCodecDescriptors.compatibilityError(
            Collections.singletonList(new ScriptInstructionCodecDescriptor(
                id("sync"), 2, Collections.<ResourceLocation>emptySet())),
            Collections.singletonList(required)).contains("Missing required capabilities"));
        assertEquals("", ScriptCodecDescriptors.compatibilityError(
            Collections.singletonList(new ScriptInstructionCodecDescriptor(
                id("sync"), 2, Arrays.asList(id("outline"), id("extra")))),
            Collections.singletonList(required)));
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("descriptor_test", path);
    }
}
