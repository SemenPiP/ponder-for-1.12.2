package net.createmod.catnip.command;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentTranslation;

public final class FlySpeedCommand {
    private FlySpeedCommand(){}
    public static void execute(MinecraftServer server,ICommandSender sender,String[] args)throws CommandException{
        if(!sender.canUseCommand(2,"catnip"))throw new CommandException("commands.generic.permission");
        if(args.length<1)throw new CommandException("Usage: /catnip flySpeed <speed|reset> [player]");
        EntityPlayerMP player=args.length>=2?server.getPlayerList().getPlayerByUsername(args[1]):net.minecraft.command.CommandBase.getCommandSenderAsPlayer(sender);
        if(player==null)throw new CommandException("commands.generic.player.notFound",args.length>=2?args[1]:"");
        float speed;
        if("reset".equalsIgnoreCase(args[0]))speed=.05f;else try{speed=Float.parseFloat(args[0]);}catch(NumberFormatException e){throw new CommandException("commands.generic.num.invalid",args[0]);}
        if(speed<0||speed>10||Float.isNaN(speed)||Float.isInfinite(speed))throw new CommandException("Speed must be between 0 and 10");
        player.capabilities.setFlySpeed(speed);player.sendPlayerAbilities();
        sender.sendMessage(new TextComponentTranslation("catnip.util.fly_speed_set.message",player.getDisplayName(),speed));
    }
}
