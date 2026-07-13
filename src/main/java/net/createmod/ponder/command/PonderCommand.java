package net.createmod.ponder.command;

import java.util.Collections;
import java.util.List;

import net.createmod.catnip.net.packets.ClientboundSimpleActionPacket;
import net.createmod.catnip.platform.CatnipServices;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.createmod.ponder.foundation.structure.PonderStructureLoader;
import net.createmod.ponder.script.ScriptSceneSync;

/** Forge 1.12 command adapter for opening and reloading Ponder on a client. */
public final class PonderCommand extends CommandBase {
    @Override public String getName() { return "ponder"; }
    @Override public String getUsage(ICommandSender sender) {
        return "/ponder <index|tags|reload|sync|namespace:path>";
    }
    @Override public int getRequiredPermissionLevel() { return 0; }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] arguments) throws CommandException {
        if (arguments.length > 0 && "sync".equalsIgnoreCase(arguments[0])) {
            if (!sender.canUseCommand(2, getName())) throw new CommandException("commands.generic.permission");
            ScriptSceneSync.sendAll(server);
            sender.sendMessage(new TextComponentString("Ponder scene snapshot sent to all online clients."));
            return;
        }
        if (arguments.length > 0 && "reload".equalsIgnoreCase(arguments[0])) {
            if (!sender.canUseCommand(2, getName())) throw new CommandException("commands.generic.permission");
            PonderStructureLoader.invalidateCaches();
            PonderIndex.reload();
            CatnipServices.NETWORK.sendToAllClients(new ClientboundSimpleActionPacket("reloadPonder", ""));
            sender.sendMessage(new TextComponentString(
                "Ponder reapplied compiled scenes and refreshed structures. ZenScript changes require a restart."));
            return;
        }
        if (!(sender.getCommandSenderEntity() instanceof EntityPlayerMP))
            throw new CommandException("ponder.command.player_only");
        EntityPlayerMP player = (EntityPlayerMP) sender.getCommandSenderEntity();
        String value = arguments.length == 0 ? "ponder:tags" : arguments[0];
        if ("index".equalsIgnoreCase(value)) value = "ponder:index";
        if ("tags".equalsIgnoreCase(value)) value = "ponder:tags";
        if (!"ponder:index".equals(value) && !"ponder:tags".equals(value)) {
            ResourceLocation id;
            try { id = new ResourceLocation(value); }
            catch (RuntimeException malformed) { throw new CommandException("ponder.command.invalid_id", value); }
            if (!PonderIndex.getSceneAccess().doScenesExistForId(id))
                throw new CommandException("ponder.command.unknown_scene", value);
        }
        CatnipServices.NETWORK.sendToClient(player, new ClientboundSimpleActionPacket("openPonder", value));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
                                          BlockPos targetPos) {
        if (args.length != 1) return Collections.emptyList();
        java.util.ArrayList<String> options = new java.util.ArrayList<String>();
        options.add("index"); options.add("tags");
        if (sender.canUseCommand(2, getName())) { options.add("reload"); options.add("sync"); }
        PonderIndex.getSceneAccess().getRegisteredEntries().forEach(entry -> options.add(entry.getKey().toString()));
        return getListOfStringsMatchingLastWord(args, options);
    }
}
