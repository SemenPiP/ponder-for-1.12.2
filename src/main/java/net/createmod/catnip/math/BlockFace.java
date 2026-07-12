package net.createmod.catnip.math;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.codecs.Codec;
import net.createmod.catnip.codecs.Codecs;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecs;
import net.createmod.catnip.codecs.stream.StreamCodec;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

public class BlockFace extends Pair<BlockPos, EnumFacing> {
    public static final Codec<BlockFace> CODEC = Codecs.compound(new Codecs.CompoundEncoder<BlockFace>() {
        public void encode(BlockFace value, NBTTagCompound tag) {
            tag.setTag("pos", NBTHelper.writeBlockPos(value.getPos()));
            NBTHelper.writeEnum(tag, "direction", value.getFace());
        }
        public BlockFace decode(NBTTagCompound tag) {
            return new BlockFace(NBTHelper.readBlockPos(tag, "pos"), NBTHelper.readEnum(tag, "direction", EnumFacing.class));
        }
    });
    public static final StreamCodec<ByteBuf, BlockFace> STREAM_CODEC = new StreamCodec<ByteBuf, BlockFace>() {
        public BlockFace decode(ByteBuf buffer) {
            return new BlockFace(CatnipStreamCodecs.BLOCK_POS.decode(buffer), CatnipStreamCodecs.DIRECTION.decode(buffer));
        }
        public void encode(ByteBuf buffer, BlockFace value) {
            CatnipStreamCodecs.BLOCK_POS.encode(buffer, value.getPos());
            CatnipStreamCodecs.DIRECTION.encode(buffer, value.getFace());
        }
    };
    public BlockFace(BlockPos pos, EnumFacing face) { super(pos, face); }
    public boolean isEquivalent(BlockFace other) {
        return equals(other) || getConnectedPos().equals(other.getPos()) && getPos().equals(other.getConnectedPos());
    }
    public BlockPos getPos() { return first; }
    public EnumFacing getFace() { return second; }
    public EnumFacing getOppositeFace() { return second.getOpposite(); }
    public BlockFace getOpposite() { return new BlockFace(getConnectedPos(), getOppositeFace()); }
    public BlockPos getConnectedPos() { return first.offset(second); }
    public NBTTagCompound serializeNBT() { return (NBTTagCompound) CODEC.encode(this); }
    public static BlockFace fromNBT(NBTTagCompound tag) { return CODEC.decode(tag); }
}
