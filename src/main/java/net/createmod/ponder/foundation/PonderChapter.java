package net.createmod.ponder.foundation;

import net.minecraft.util.ResourceLocation;

public final class PonderChapter {
    private final ResourceLocation id;
    private final ResourceLocation icon;

    private PonderChapter(ResourceLocation id) {
        this.id = id;
        this.icon = new ResourceLocation(id.getNamespace(), "textures/ponder/chapter/" + id.getPath() + ".png");
    }

    public static PonderChapter of(ResourceLocation id) {
        return new PonderChapter(id);
    }

    public ResourceLocation getId() { return id; }
    public ResourceLocation getIcon() { return icon; }
    public String getTitle() { return id.getPath(); }
    @Override public boolean equals(Object obj) { return obj == this || obj instanceof PonderChapter && id.equals(((PonderChapter) obj).id); }
    @Override public int hashCode() { return id.hashCode(); }
}
