package net.createmod.ponder.foundation;

import javax.annotation.Nullable;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public final class PonderTag {
    public static final class Highlight {
        public static final ResourceLocation ALL = new ResourceLocation("ponder", "_all");

        private Highlight() {
        }
    }

    private final ResourceLocation id;
    @Nullable private final ResourceLocation textureIconLocation;
    private final ItemStack itemIcon;
    private final ItemStack mainItem;

    public PonderTag(ResourceLocation id, @Nullable ResourceLocation textureIconLocation, ItemStack itemIcon,
                     ItemStack mainItem) {
        if (id == null)
            throw new IllegalArgumentException("Tag id is required");
        this.id = id;
        this.textureIconLocation = textureIconLocation;
        this.itemIcon = itemIcon == null ? ItemStack.EMPTY : itemIcon.copy();
        this.mainItem = mainItem == null ? ItemStack.EMPTY : mainItem.copy();
    }

    public static PonderTag missing(ResourceLocation requested) {
        return new PonderTag(requested == null ? new ResourceLocation("ponder", "not_registered") : requested,
            null, new ItemStack(Blocks.BARRIER), new ItemStack(Blocks.BARRIER));
    }

    public ResourceLocation getId() { return id; }
    @Nullable public ResourceLocation getTextureIconLocation() { return textureIconLocation; }
    public ItemStack getItemIcon() { return itemIcon.copy(); }
    public ItemStack getMainItem() { return mainItem.copy(); }
    public String getTitle() { return PonderIndex.getLangAccess().getTagName(id); }
    public String getDescription() { return PonderIndex.getLangAccess().getTagDescription(id); }

    @Override
    public boolean equals(Object obj) {
        return obj == this || obj instanceof PonderTag && id.equals(((PonderTag) obj).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "PonderTag{" + id + "}";
    }
}
