package net.createmod.catnip.command;

import net.createmod.catnip.config.ConfigPath;
import net.createmod.catnip.config.ConfigRegistry;
import net.createmod.catnip.net.packets.ClientboundSimpleActionPacket;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;

public final class ConfigCommand {
    private ConfigCommand(){}
    public static void execute(ICommandSender sender,String[] args)throws CommandException{
        if(args.length==0){EntityPlayerMP player=net.minecraft.command.CommandBase.getCommandSenderAsPlayer(sender);CatnipServices.NETWORK.sendToClient(player,new ClientboundSimpleActionPacket("configScreen",""));return;}
        if("get".equalsIgnoreCase(args[0])&&args.length==2){try{ConfigPath path=ConfigPath.parse(args[1]);sender.sendMessage(new TextComponentString(path+" = "+ConfigRegistry.getSerialized(path)));return;}catch(IllegalArgumentException e){throw new CommandException(e.getMessage());}}
        if("set".equalsIgnoreCase(args[0])&&args.length>=3){if(!sender.canUseCommand(2,"catnip"))throw new CommandException("commands.generic.permission");try{ConfigPath path=ConfigPath.parse(args[1]);ConfigRegistry.set(path,join(args,2));sender.sendMessage(new TextComponentString("Updated "+path));return;}catch(IllegalArgumentException e){throw new CommandException(e.getMessage());}}
        if(args.length==1){EntityPlayerMP player=net.minecraft.command.CommandBase.getCommandSenderAsPlayer(sender);CatnipServices.NETWORK.sendToClient(player,new ClientboundSimpleActionPacket("configScreen",args[0]));return;}
        throw new CommandException("Usage: /catnip config [path|get <path>|set <path> <value>]");
    }
    private static String join(String[] values,int from){StringBuilder result=new StringBuilder();for(int i=from;i<values.length;i++){if(result.length()>0)result.append(' ');result.append(values[i]);}return result.toString();}
}
