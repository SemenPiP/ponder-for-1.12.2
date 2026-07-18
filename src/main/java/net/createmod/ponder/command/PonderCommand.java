package net.createmod.ponder.command;

import java.util.Collections;
import java.util.List;

import net.createmod.catnip.net.packets.ClientboundSimpleActionPacket;
import net.createmod.catnip.platform.CatnipServices;
import net.createmod.ponder.api.diagnostic.PonderDiagnosticView;
import net.createmod.ponder.api.diagnostic.PonderDiagnostics;
import net.createmod.ponder.api.diagnostic.PonderSyncDiagnostic;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.diagnostic.PonderDiagnosticService;
import net.createmod.ponder.foundation.structure.PonderStructureLoader;
import net.createmod.ponder.script.ScriptSceneSync;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

/** Forge 1.12 command adapter for opening and reloading Ponder on a client. */
public final class PonderCommand extends CommandBase {
    @Override public String getName() { return "ponder"; }
    @Override public String getUsage(ICommandSender sender) {
        return "/ponder <index|tags|reload|sync|list|inspect|validate|export|namespace:path>";
    }
    @Override public int getRequiredPermissionLevel() { return 0; }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] arguments) throws CommandException {
        if (arguments.length > 0 && "sync".equalsIgnoreCase(arguments[0])) {
            if (!sender.canUseCommand(2, getName())) throw new CommandException("commands.generic.permission");
            if (arguments.length > 1 && "status".equalsIgnoreCase(arguments[1])) {
                String playerFilter = arguments.length > 2 ? arguments[2] : "";
                int count = 0;
                for (PonderSyncDiagnostic status : PonderDiagnostics.syncStatuses()) {
                    if (!playerFilter.isEmpty() && !status.getPlayerName().equalsIgnoreCase(playerFilter))
                        continue;
                    sender.sendMessage(new TextComponentString(status.getPlayerName() + ": "
                        + status.getStatus() + " transfer=" + status.getTransferId()
                        + " protocol=" + status.getProtocol() + " codecs=" + status.getCodecs().size()
                        + " compressed=" + status.getCompressedBytes()
                        + " uncompressed=" + status.getUncompressedBytes()
                        + " started=" + status.getStartedAt()
                        + " updated=" + status.getUpdatedAt()
                        + (status.getLastResult().isEmpty() ? "" : " result=" + status.getLastResult())));
                    count++;
                }
                if (count == 0)
                    sender.sendMessage(new TextComponentString("No matching Ponder sync status is available."));
                return;
            }
            ScriptSceneSync.sendAll(server);
            sender.sendMessage(new TextComponentString("Ponder scene snapshot sent to all online clients."));
            return;
        }
        if (arguments.length > 0 && isDiagnostic(arguments[0])) {
            String request = join(arguments);
            if (sender.getCommandSenderEntity() instanceof EntityPlayerMP) {
                CatnipServices.NETWORK.sendToClient((EntityPlayerMP) sender.getCommandSenderEntity(),
                    new ClientboundSimpleActionPacket("ponderDiagnostic", request));
                return;
            }
            if (("validate".equalsIgnoreCase(arguments[0]) || "export".equalsIgnoreCase(arguments[0]))
                && !sender.canUseCommand(2, getName()))
                throw new CommandException("commands.generic.permission");
            try {
                PonderDiagnosticService.execute("server", request,
                    message -> sender.sendMessage(new TextComponentString(message)));
            } catch (RuntimeException failure) {
                throw new CommandException(failure.getMessage());
            }
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
        if (args.length == 1) {
            java.util.ArrayList<String> options = new java.util.ArrayList<String>();
            options.add("index"); options.add("tags"); options.add("list"); options.add("inspect");
            options.add("validate"); options.add("export");
            if (sender.canUseCommand(2, getName())) { options.add("reload"); options.add("sync"); }
            PonderIndex.getSceneAccess().getRegisteredEntries()
                .forEach(entry -> options.add(entry.getKey().toString()));
            return getListOfStringsMatchingLastWord(args, options);
        }
        if (args.length == 2 && ("list".equalsIgnoreCase(args[0])
            || "validate".equalsIgnoreCase(args[0])))
            return getListOfStringsMatchingLastWord(args, "local", "server", "effective");
        if (args.length == 2 && ("inspect".equalsIgnoreCase(args[0])
            || "export".equalsIgnoreCase(args[0])))
            return getListOfStringsMatchingLastWord(args,
                PonderDiagnosticService.sceneIds(PonderDiagnosticView.EFFECTIVE));
        if (args.length == 3 && "inspect".equalsIgnoreCase(args[0]))
            return getListOfStringsMatchingLastWord(args, "local", "server", "effective");
        if (args.length == 3 && "export".equalsIgnoreCase(args[0]))
            return getListOfStringsMatchingLastWord(args, "ir", "timeline", "all");
        if (args.length == 4 && "export".equalsIgnoreCase(args[0]))
            return getListOfStringsMatchingLastWord(args, "local", "server", "effective");
        if (args.length == 2 && "sync".equalsIgnoreCase(args[0]))
            return getListOfStringsMatchingLastWord(args, "status");
        if (args.length == 3 && "sync".equalsIgnoreCase(args[0])
            && "status".equalsIgnoreCase(args[1]))
            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        return Collections.emptyList();
    }

    private static boolean isDiagnostic(String command) {
        return "list".equalsIgnoreCase(command) || "inspect".equalsIgnoreCase(command)
            || "validate".equalsIgnoreCase(command) || "export".equalsIgnoreCase(command);
    }

    private static String join(String[] arguments) {
        StringBuilder result = new StringBuilder();
        for (String argument : arguments) {
            if (result.length() > 0)
                result.append(' ');
            result.append(argument);
        }
        return result.toString();
    }
}
