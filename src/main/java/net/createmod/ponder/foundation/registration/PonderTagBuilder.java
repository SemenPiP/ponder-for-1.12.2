package net.createmod.ponder.foundation.registration;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.createmod.ponder.api.registration.TagBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public final class PonderTagBuilder implements TagBuilder {
    final ResourceLocation id;
    final Consumer<PonderTagBuilder> onFinish;
    String title = "Untitled";
    String description = "";
    boolean addToIndex;
    boolean registered;
    @Nullable ResourceLocation textureIconLocation;
    ItemStack itemIcon = ItemStack.EMPTY;
    ItemStack mainItem = ItemStack.EMPTY;

    public PonderTagBuilder(ResourceLocation id, Consumer<PonderTagBuilder> onFinish) {
        this.id = id;
        this.onFinish = onFinish;
    }

    @Override public TagBuilder title(String title) { this.title = title; return this; }
    @Override public TagBuilder description(String description) { this.description = description; return this; }
    @Override public TagBuilder addToIndex() { addToIndex = true; return this; }

    @Override
    public TagBuilder icon(ResourceLocation location) {
        textureIconLocation = new ResourceLocation(location.getNamespace(),
            "textures/ponder/tag/" + location.getPath() + ".png");
        return this;
    }

    @Override
    public TagBuilder icon(String path) {
        return icon(new ResourceLocation(id.getNamespace(), path));
    }

    @Override public TagBuilder idAsIcon() { return icon(id); }

    @Override
    public TagBuilder item(ItemStack stack, boolean useAsIcon, boolean useAsMainItem) {
        if (stack == null)
            throw new IllegalArgumentException("Tag item may not be null");
        if (useAsIcon) itemIcon = stack.copy();
        if (useAsMainItem) mainItem = stack.copy();
        return this;
    }

    @Override
    public void register() {
        if (registered)
            throw new IllegalStateException("Tag builder already registered: " + id);
        registered = true;
        onFinish.accept(this);
    }
}
