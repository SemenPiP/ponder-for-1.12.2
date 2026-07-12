package net.createmod.catnip.codecs.stream;

import java.io.IOException;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public final class CatnipStreamCodecs {
    private CatnipStreamCodecs() {}
    public static final StreamCodec<ByteBuf, Boolean> BOOL = simpleBoolean();
    public static final StreamCodec<ByteBuf, Integer> INT = simpleInt();
    public static final StreamCodec<ByteBuf, Integer> VAR_INT = varInt();
    public static final StreamCodec<ByteBuf, Long> LONG = simpleLong();
    public static final StreamCodec<ByteBuf, Float> FLOAT = simpleFloat();
    public static final StreamCodec<ByteBuf, Double> DOUBLE = simpleDouble();
    public static final StreamCodec<ByteBuf, Character> CHAR = new StreamCodec<ByteBuf, Character>() {
        public Character decode(ByteBuf b) { return b.readChar(); }
        public void encode(ByteBuf b, Character v) { b.writeChar(v); }
    };
    public static final StreamCodec<ByteBuf, String> STRING_UTF8 = CatnipStreamCodecBuilders.string(32767);
    public static final StreamCodec<ByteBuf, ResourceLocation> RESOURCE_LOCATION = STRING_UTF8.map(
        new java.util.function.Function<String, ResourceLocation>() { public ResourceLocation apply(String v) { return new ResourceLocation(v); } },
        new java.util.function.Function<ResourceLocation, String>() { public String apply(ResourceLocation v) { return v.toString(); } });
    public static final StreamCodec<ByteBuf, Vec3d> VEC3 = new StreamCodec<ByteBuf, Vec3d>() {
        public Vec3d decode(ByteBuf b) { return new Vec3d(b.readDouble(), b.readDouble(), b.readDouble()); }
        public void encode(ByteBuf b, Vec3d v) { b.writeDouble(v.x); b.writeDouble(v.y); b.writeDouble(v.z); }
    };
    public static final StreamCodec<ByteBuf, Vec3i> VEC3I = new StreamCodec<ByteBuf, Vec3i>() {
        public Vec3i decode(ByteBuf b) { return new Vec3i(b.readInt(), b.readInt(), b.readInt()); }
        public void encode(ByteBuf b, Vec3i v) { b.writeInt(v.getX()); b.writeInt(v.getY()); b.writeInt(v.getZ()); }
    };
    public static final StreamCodec<ByteBuf, BlockPos> BLOCK_POS = new StreamCodec<ByteBuf, BlockPos>() {
        public BlockPos decode(ByteBuf b) { return BlockPos.fromLong(b.readLong()); }
        public void encode(ByteBuf b, BlockPos v) { b.writeLong(v.toLong()); }
    };
    public static final StreamCodec<ByteBuf, BlockPos> NULLABLE_BLOCK_POS = CatnipStreamCodecBuilders.nullable(BLOCK_POS);
    public static final StreamCodec<ByteBuf, EnumFacing> DIRECTION = CatnipStreamCodecBuilders.ofEnum(EnumFacing.class);
    public static final StreamCodec<ByteBuf, EnumFacing.Axis> AXIS = CatnipStreamCodecBuilders.ofEnum(EnumFacing.Axis.class);
    public static final StreamCodec<ByteBuf, Rotation> ROTATION = CatnipStreamCodecBuilders.ofEnum(Rotation.class);
    public static final StreamCodec<ByteBuf, Mirror> MIRROR = CatnipStreamCodecBuilders.ofEnum(Mirror.class);
    public static final StreamCodec<ByteBuf, EnumHand> HAND = CatnipStreamCodecBuilders.ofEnum(EnumHand.class);
    public static final StreamCodec<ByteBuf, EntityEquipmentSlot> EQUIPMENT_SLOT = CatnipStreamCodecBuilders.ofEnum(EntityEquipmentSlot.class);
    public static final StreamCodec<ByteBuf, IBlockState> BLOCK_STATE = new StreamCodec<ByteBuf, IBlockState>() {
        public IBlockState decode(ByteBuf b) {
            IBlockState state = Block.BLOCK_STATE_IDS.getByValue(CatnipStreamCodecBuilders.readVarInt(b));
            if (state == null) throw new IllegalArgumentException("Unknown block state id");
            return state;
        }
        public void encode(ByteBuf b, IBlockState v) { CatnipStreamCodecBuilders.writeVarInt(b, Block.BLOCK_STATE_IDS.get(v)); }
    };
    public static final StreamCodec<PacketBuffer, NBTTagCompound> COMPOUND_TAG = new StreamCodec<PacketBuffer, NBTTagCompound>() {
        public NBTTagCompound decode(PacketBuffer b) {
            try { return b.readCompoundTag(); } catch (IOException e) { throw new IllegalArgumentException("Invalid NBT payload", e); }
        }
        public void encode(PacketBuffer b, NBTTagCompound v) { b.writeCompoundTag(v); }
    };
    public static final StreamCodec<PacketBuffer, ItemStack> ITEM_STACK = new StreamCodec<PacketBuffer, ItemStack>() {
        public ItemStack decode(PacketBuffer b) { try { return b.readItemStack(); } catch (IOException e) { throw new IllegalArgumentException("Invalid item stack", e); } }
        public void encode(PacketBuffer b, ItemStack v) { b.writeItemStack(v); }
    };

    private static StreamCodec<ByteBuf, Boolean> simpleBoolean() { return new StreamCodec<ByteBuf, Boolean>() { public Boolean decode(ByteBuf b) { return b.readBoolean(); } public void encode(ByteBuf b, Boolean v) { b.writeBoolean(v); } }; }
    private static StreamCodec<ByteBuf, Integer> simpleInt() { return new StreamCodec<ByteBuf, Integer>() { public Integer decode(ByteBuf b) { return b.readInt(); } public void encode(ByteBuf b, Integer v) { b.writeInt(v); } }; }
    private static StreamCodec<ByteBuf, Integer> varInt() { return new StreamCodec<ByteBuf, Integer>() { public Integer decode(ByteBuf b) { return CatnipStreamCodecBuilders.readVarInt(b); } public void encode(ByteBuf b, Integer v) { CatnipStreamCodecBuilders.writeVarInt(b, v); } }; }
    private static StreamCodec<ByteBuf, Long> simpleLong() { return new StreamCodec<ByteBuf, Long>() { public Long decode(ByteBuf b) { return b.readLong(); } public void encode(ByteBuf b, Long v) { b.writeLong(v); } }; }
    private static StreamCodec<ByteBuf, Float> simpleFloat() { return new StreamCodec<ByteBuf, Float>() { public Float decode(ByteBuf b) { return b.readFloat(); } public void encode(ByteBuf b, Float v) { b.writeFloat(v); } }; }
    private static StreamCodec<ByteBuf, Double> simpleDouble() { return new StreamCodec<ByteBuf, Double>() { public Double decode(ByteBuf b) { return b.readDouble(); } public void encode(ByteBuf b, Double v) { b.writeDouble(v); } }; }
}
