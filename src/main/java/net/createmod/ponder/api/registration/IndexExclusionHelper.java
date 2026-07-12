package net.createmod.ponder.api.registration;

import java.util.function.Predicate;

import net.minecraft.block.Block;
import net.minecraft.item.Item;

public interface IndexExclusionHelper {
    IndexExclusionHelper exclude(Item item);

    IndexExclusionHelper exclude(Block block);

    IndexExclusionHelper excludeItemVariants(Class<? extends Item> itemClass, Item originalVariant);

    IndexExclusionHelper excludeBlockVariants(Class<? extends Block> blockClass, Block originalVariant);

    IndexExclusionHelper exclude(Predicate<Item> predicate);

    boolean isExcluded(Item item);
}
