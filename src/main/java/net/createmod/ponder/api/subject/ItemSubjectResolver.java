package net.createmod.ponder.api.subject;

import net.minecraft.item.ItemStack;

/**
 * Resolves an item stack to the component id used by the Ponder scene registry.
 */
@FunctionalInterface
public interface ItemSubjectResolver {
    SubjectResolverResult resolve(ItemStack stack);
}
