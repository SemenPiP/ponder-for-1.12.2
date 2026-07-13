package net.createmod.ponder.script;

import net.createmod.ponder.Ponder;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.createmod.ponder.api.registration.SharedTextRegistrationHelper;
import net.createmod.ponder.api.registration.IndexExclusionHelper;
import net.createmod.ponder.api.registration.TagBuilder;
import net.createmod.ponder.foundation.structure.PonderStructureLoader;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.FMLCommonHandler;

public final class ScriptPonderPlugin implements PonderPlugin {
    @Override
    public String getModId() {
        return Ponder.CONTENT_NAMESPACE;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        for (ScriptSceneDefinition definition : ScriptSceneRegistry.effectiveScenes()) {
            if (FMLCommonHandler.instance().getSide().isClient()) {
                try {
                    new PonderStructureLoader().load(definition.getStructure());
                } catch (java.io.IOException missing) {
                    Ponder.LOGGER.error("Skipping Ponder script scene {} because structure {} is unavailable; expected {}",
                        definition.getSceneId(), definition.getStructure(),
                        PonderStructureLoader.expectedExternalPath(definition.getStructure()), missing);
                    ScriptMissingStructures.record(definition.getSceneId(), definition.getStructure());
                    continue;
                }
            }
            helper.addStoryBoard(definition.getComponent(), definition.getStructure(), definition.asStoryBoard(),
                definition.getTags().toArray(new ResourceLocation[0]));
        }
    }

    @Override
    public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        for (ScriptTagDefinition definition : ScriptTagRegistry.snapshot()) {
            TagBuilder builder = helper.registerTag(definition.id)
                .title(definition.title)
                .description(definition.description);
            Item icon = Item.REGISTRY.getObject(definition.icon);
            if (icon != null) builder.item(new ItemStack(icon));
            else builder.icon(definition.icon);
            if (definition.indexed) builder.addToIndex();
            builder.register();
            for (ResourceLocation component : definition.components)
                helper.addTagToComponent(component, definition.id);
        }
    }

    @Override
    public void registerSharedText(SharedTextRegistrationHelper helper) {
        for (java.util.Map.Entry<String, String> entry : ScriptSharedText.snapshot().entrySet())
            helper.registerSharedText(entry.getKey(), entry.getValue());
    }

    @Override
    public void indexExclusions(IndexExclusionHelper helper) {
        for (ResourceLocation id : ScriptIndex.snapshot()) {
            Item item = Item.REGISTRY.getObject(id);
            if (item != null) helper.exclude(item);
        }
    }
}
