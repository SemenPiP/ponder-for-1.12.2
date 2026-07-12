package net.createmod.catnip.command;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

public class CatnipCommand extends CommandBase {
    public String getName(){return "catnip";} public String getUsage(ICommandSender sender){return "/catnip <config|flySpeed>";}
    public List<String> getAliases(){return Collections.singletonList("c");} public int getRequiredPermissionLevel(){return 0;}
    public void execute(MinecraftServer server,ICommandSender sender,String[] args)throws CommandException{
        if(args.length==0)throw new CommandException(getUsage(sender));
        String[] tail=Arrays.copyOfRange(args,1,args.length);
        if("config".equalsIgnoreCase(args[0]))ConfigCommand.execute(sender,tail);
        else if("flySpeed".equalsIgnoreCase(args[0])||"flyspeed".equalsIgnoreCase(args[0]))FlySpeedCommand.execute(server,sender,tail);
        else throw new CommandException(getUsage(sender));
    }
    public List<String> getTabCompletions(MinecraftServer server,ICommandSender sender,String[] args,BlockPos pos){if(args.length==1)return getListOfStringsMatchingLastWord(args,"config","flySpeed");if(args.length==3&&"flySpeed".equalsIgnoreCase(args[0]))return getListOfStringsMatchingLastWord(args,server.getOnlinePlayerNames());return Collections.emptyList();}
}
