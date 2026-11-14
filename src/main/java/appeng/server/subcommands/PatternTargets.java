package appeng.server.subcommands;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import appeng.api.crafting.PatternMatch;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.util.DimensionalCoord;
import appeng.api.util.IInterfaceViewable;
import appeng.crafting.target.PatternTargetQuery;
import appeng.server.ISubCommand;
import appeng.util.Platform;

public class PatternTargets implements ISubCommand {

    private static final int MAX_RUNS = 1000;

    // Same order as GuiInterfaceTerminal#translateRawName, but the key goes to the client, so a dedicated server still
    // shows the player's language. CRIB, for one, reports the machine's key without ".name".
    private static IChatComponent providerName(IInterfaceViewable provider) {
        final String rawName = provider.getRawName();
        if (StatCollector.canTranslate(rawName)) {
            return new ChatComponentTranslation(rawName);
        }
        if (StatCollector.canTranslate(rawName + ".name")) {
            return new ChatComponentTranslation(rawName + ".name");
        }
        final ItemStack rep = provider.getDisplayRep();
        return new ChatComponentText(rep != null ? rep.getDisplayName() : rawName);
    }

    private static int[] parseCoords(String[] args) {
        if (args.length < 4) {
            return null;
        }
        try {
            return new int[] { Integer.decode(args[1]), Integer.decode(args[2]), Integer.decode(args[3]) };
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String matchKey(PatternMatch match) {
        return switch (match) {
            case MATCH -> "commands.ae2.PatternTargetsMatch";
            case NO_MATCH -> "commands.ae2.PatternTargetsNoMatch";
            case UNKNOWN -> "commands.ae2.PatternTargetsUnknown";
        };
    }

    @Override
    public String getHelp(MinecraftServer srv) {
        return "commands.ae2.PatternTargets";
    }

    @Override
    public void call(MinecraftServer srv, String[] args, ICommandSender sender) {
        if (!(sender instanceof EntityPlayer player)) {
            sender.addChatMessage(new ChatComponentTranslation("commands.ae2.PatternTargets"));
            return;
        }

        final ItemStack held = player.getHeldItem();
        final ICraftingPatternDetails pattern = held != null && held.getItem() instanceof ICraftingPatternItem item
                ? item.getPatternForItem(held, player.worldObj)
                : null;
        if (pattern == null) {
            sender.addChatMessage(new ChatComponentTranslation("commands.ae2.PatternTargetsNoPattern"));
            return;
        }

        int firstOption = 1;
        final TileEntity tile;
        final int[] coords = parseCoords(args);
        if (coords != null) {
            tile = player.worldObj.getTileEntity(coords[0], coords[1], coords[2]);
            firstOption = 4;
        } else {
            final MovingObjectPosition hit = Platform.rayTrace(player, true, false);
            tile = hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                    ? player.worldObj.getTileEntity(hit.blockX, hit.blockY, hit.blockZ)
                    : null;
        }

        final IGridNode node = tile instanceof IGridHost host ? host.getGridNode(ForgeDirection.UNKNOWN) : null;
        if (node == null) {
            sender.addChatMessage(new ChatComponentTranslation("commands.ae2.ProfilerFailed"));
            return;
        }
        final IGrid grid = node.getGrid();
        if (grid == null) {
            sender.addChatMessage(new ChatComponentTranslation("commands.ae2.ProfilerGridDown"));
            return;
        }

        boolean list = false;
        int runs = 1;
        for (int i = firstOption; i < args.length; i++) {
            if ("list".equalsIgnoreCase(args[i])) {
                list = true;
            } else if (args[i].length() > 1 && Character.toLowerCase(args[i].charAt(0)) == 'x') {
                try {
                    runs = Integer.parseInt(args[i].substring(1));
                } catch (NumberFormatException e) {
                    sender.addChatMessage(new ChatComponentTranslation("commands.ae2.PatternTargets"));
                    return;
                }
                // The runs block the server thread, so a typo must not freeze it for minutes.
                runs = Math.max(1, Math.min(runs, MAX_RUNS));
            }
        }

        PatternTargetQuery.Result result = null;
        long totalNanos = 0;
        long minNanos = Long.MAX_VALUE;
        for (int i = 0; i < runs; i++) {
            result = PatternTargetQuery.run(grid, pattern);
            totalNanos += result.nanos;
            minNanos = Math.min(minNanos, result.nanos);
        }

        int matches = 0;
        int noMatches = 0;
        int unknown = 0;
        for (PatternTargetQuery.Entry entry : result.entries) {
            switch (entry.match) {
                case MATCH -> matches++;
                case NO_MATCH -> noMatches++;
                case UNKNOWN -> unknown++;
            }
            if (list) {
                final DimensionalCoord location = entry.provider.getLocation();
                sender.addChatMessage(
                        new ChatComponentTranslation(
                                "commands.ae2.PatternTargetsEntry",
                                providerName(entry.provider),
                                location.x,
                                location.y,
                                location.z,
                                new ChatComponentTranslation(matchKey(entry.match)),
                                entry.freeSlots));
            }
        }

        if (list) {
            for (PatternTargetQuery.Conflict conflict : PatternTargetQuery.findConflicts(grid, pattern)) {
                final DimensionalCoord location = conflict.provider.getLocation();
                sender.addChatMessage(
                        new ChatComponentTranslation(
                                "commands.ae2.PatternTargetsConflict",
                                providerName(conflict.provider),
                                location.x,
                                location.y,
                                location.z,
                                conflict.slot,
                                new ChatComponentTranslation(
                                        conflict.sameRecipe ? "commands.ae2.PatternTargetsSameRecipe"
                                                : "commands.ae2.PatternTargetsOtherRecipe")));
            }
        }

        sender.addChatMessage(
                new ChatComponentTranslation(
                        "commands.ae2.PatternTargetsResult",
                        result.entries.size(),
                        result.distinctKeys,
                        matches,
                        noMatches,
                        unknown,
                        result.nanos / 1000));
        if (runs > 1) {
            sender.addChatMessage(
                    new ChatComponentTranslation(
                            "commands.ae2.PatternTargetsAverage",
                            runs,
                            totalNanos / runs / 1000,
                            minNanos / 1000));
        }
    }
}
