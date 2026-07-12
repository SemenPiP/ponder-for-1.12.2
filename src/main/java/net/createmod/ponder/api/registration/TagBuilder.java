package net.createmod.ponder.api.registration;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public interface TagBuilder {
    TagBuilder title(String title);

    TagBuilder description(String description);

    TagBuilder addToIndex();

    TagBuilder icon(ResourceLocation location);

    TagBuilder icon(String path);

    TagBuilder idAsIcon();

    TagBuilder item(ItemStack item, boolean useAsIcon, boolean useAsMainItem);

    default TagBuilder item(Item item, boolean useAsIcon, boolean useAsMainItem) {
        return item(new ItemStack(item), useAsIcon, useAsMainItem);
    }

    default TagBuilder item(Block block, boolean useAsIcon, boolean useAsMainItem) {
        return item(new ItemStack(block), useAsIcon, useAsMainItem);
    }

    default TagBuilder item(Item item) {
        return item(item, true, true);
    }

    default TagBuilder item(Block block) {
        return item(block, true, true);
    }

    default TagBuilder item(ItemStack stack) {
        return item(stack, true, true);
    }

    void register();
}
