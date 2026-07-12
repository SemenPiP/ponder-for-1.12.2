package net.createmod.catnip.codecs;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.codecs.stream.StreamCodec;
import net.minecraft.nbt.NBTBase;

public class CodecStreamTest {
    @Test public void nbtListCodecRoundTrips(){
        Codec<List<Integer>> codec=Codecs.list(Codecs.INT,4);List<Integer> input=Arrays.asList(1,2,3);NBTBase encoded=codec.encode(input);assertEquals(input,codec.decode(encoded));
    }
    @Test(expected=CodecException.class) public void nbtListCodecRejectsOversize(){Codecs.list(Codecs.INT,1).encode(Arrays.asList(1,2));}
    @Test public void streamStringUsesUtf8ByteLimit(){
        StreamCodec<ByteBuf,String> codec=CatnipStreamCodecBuilders.string(8);ByteBuf buffer=Unpooled.buffer();codec.encode(buffer,"hello");assertEquals("hello",codec.decode(buffer));
        try{codec.encode(buffer,"123456789");fail("Expected length failure");}catch(IllegalArgumentException expected){}finally{buffer.release();}
    }
    @Test public void enumCodecRejectsUntrustedOrdinal(){
        StreamCodec<ByteBuf,Sample> codec=CatnipStreamCodecBuilders.ofEnum(Sample.class);ByteBuf buffer=Unpooled.buffer();CatnipStreamCodecBuilders.writeVarInt(buffer,99);
        try{codec.decode(buffer);fail("Expected invalid ordinal");}catch(IllegalArgumentException expected){}finally{buffer.release();}
    }
    private enum Sample{ONE,TWO}
}
