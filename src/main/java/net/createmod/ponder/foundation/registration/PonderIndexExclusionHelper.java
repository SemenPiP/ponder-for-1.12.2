package net.createmod.ponder.foundation.registration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import net.createmod.ponder.api.registration.IndexExclusionHelper;
import net.minecraft.block.Block;
import net.minecraft.item.Item;

public final class PonderIndexExclusionHelper implements IndexExclusionHelper {
    private final List<Predicate<Item>> predicates = new ArrayList<Predicate<Item>>();

    @Override
    public IndexExclusionHelper exclude(final Item item) {
        if (item != null)
            predicates.add(new Predicate<Item>() {
                @Override public boolean test(Item candidate) { return candidate == item; }
            });
        return this;
    }

    @Override
    public IndexExclusionHelper exclude(Block block) {
        return block == null ? this : exclude(Item.getItemFromBlock(block));
    }

    @Override
    public IndexExclusionHelper excludeItemVariants(final Class<? extends Item> itemClass, final Item originalVariant) {
        if (itemClass != null)
            predicates.add(new Predicate<Item>() {
                @Override public boolean test(Item item) {
                    return item != originalVariant && itemClass.isInstance(item);
                }
            });
        return this;
    }

    @Override
    public IndexExclusionHelper excludeBlockVariants(final Class<? extends Block> blockClass, Block originalVariant) {
        final Item originalItem = originalVariant == null ? null : Item.getItemFromBlock(originalVariant);
        predicates.add(new Predicate<Item>() {
            @Override public boolean test(Item item) {
                Block block = Block.getBlockFromItem(item);
                return item != originalItem && block != null && blockClass.isInstance(block);
            }
        });
        return this;
    }

    @Override
    public IndexExclusionHelper exclude(Predicate<Item> predicate) {
        if (predicate != null)
            predicates.add(predicate);
        return this;
    }

    @Override
    public boolean isExcluded(Item item) {
        for (Predicate<Item> predicate : predicates)
            if (predicate.test(item))
                return true;
        return false;
    }
}
