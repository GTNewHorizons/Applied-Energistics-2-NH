package appeng.gametests.interfaces;

import static appeng.gametests.AEGameTestHelpers.assertActive;
import static appeng.gametests.AEGameTestHelpers.assertInactive;
import static appeng.gametests.AEGameTestHelpers.part;

import java.lang.reflect.Field;
import java.util.Map;

import net.minecraft.init.Blocks;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.InventoryHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import appeng.api.AEApi;
import appeng.api.util.AEColor;
import appeng.container.ContainerOpenContext;
import appeng.container.PrimaryGui;
import appeng.container.RemoteInterfaceGui;
import appeng.container.implementations.ContainerInterface;
import appeng.container.implementations.ContainerInterfaceTerminal;
import appeng.container.implementations.ContainerPriority;
import appeng.container.implementations.ContainerRemoteInterface;
import appeng.container.slot.SlotInaccessible;
import appeng.core.AppEng;
import appeng.core.sync.packets.PacketRemoteRename;
import appeng.helpers.IInterfaceHost;
import appeng.helpers.IPriorityHost;
import appeng.parts.AEBasePart;
import appeng.parts.misc.PartInterface;
import appeng.parts.reporting.PartInterfaceTerminal;
import appeng.tile.misc.TileInterface;
import appeng.tile.networking.TileCableBus;
import appeng.tile.networking.TileController;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

@GameTestHolder(AppEng.MOD_ID)
public class RemoteInterfaceTests {

    @GameTest(template = "interface_network", timeoutTicks = 100)
    public static void terminalAuthorizesOnlyItsPlayerAndListedHosts(GameTestHelper helper) {
        Network network = installTerminal(helper, "remote_authorization");
        FakePlayer stranger = helper.spawnFakePlayer("remote_stranger");

        helper.startSequence()
                .thenWaitUntil("wait for remote interface network", 80, () -> assertNetworkActive(helper, network))
                .thenExecute("check authorization and rejected packet IDs", () -> {
                    ContainerInterfaceTerminal source = terminalContainer(network);
                    assertListedHostsAccessible(helper, network, source);
                    helper.assertFalse(
                            source.canAccessInterface(stranger, network.blockInterface),
                            "Another player must not use the terminal owner's authorization");
                    helper.assertFalse(
                            source.canAccessInterface(network.player, new TileInterface()),
                            "An untracked interface must not be authorized");

                    assertRejectedId(helper, source, network.player, Long.MAX_VALUE);
                    long listedId = trackedId(source, network.blockInterface);
                    Container previous = network.player.openContainer;
                    try {
                        ContainerInterface other = new ContainerInterface(
                                network.player.inventory,
                                network.blockInterface);
                        network.player.openContainer = other;
                        source.openInterface(network.player, listedId);
                        helper.assertSame(
                                other,
                                network.player.openContainer,
                                "A packet for a non-open terminal is ignored");
                    } finally {
                        network.player.openContainer = previous;
                    }
                }).thenSucceed();
    }

    @GameTest(template = "interface_network", timeoutTicks = 100)
    public static void removedTileAndPartRejectStaleIds(GameTestHelper helper) {
        Network network = installTerminal(helper, "remote_removal");

        helper.startSequence()
                .thenWaitUntil("wait for remote interface network", 80, () -> assertNetworkActive(helper, network))
                .thenExecute("reject removed hosts before refreshing the terminal list", () -> {
                    ContainerInterfaceTerminal source = terminalContainer(network);
                    assertListedHostsAccessible(helper, network, source);
                    long tileId = trackedId(source, network.blockInterface);
                    long partId = trackedId(source, network.partInterface);

                    helper.destroyBlock("block_interface");
                    helper.assertFalse(
                            source.canAccessInterface(network.player, network.blockInterface),
                            "A removed tile must not remain accessible through the cached list");
                    assertRejectedId(helper, source, network.player, tileId);

                    ForgeDirection side = network.partInterface.getSide();
                    network.partInterface.getHost().removePart(side, true);
                    helper.assertFalse(
                            source.canAccessInterface(network.player, network.partInterface),
                            "A removed part must not remain accessible through the cached list");
                    assertRejectedId(helper, source, network.player, partId);
                    helper.assertEquals(
                            side,
                            network.partInterface.getHost().addPart(
                                    AEApi.instance().definitions().parts().iface().maybeStack(1).get(),
                                    side,
                                    null),
                            "A replacement interface should fit the vacated side");
                    IInterfaceHost replacement = part(helper, "part_interface_host", PartInterface.class);
                    helper.assertNotSame(network.partInterface, replacement, "Replacement must have a new identity");
                    helper.assertFalse(
                            source.canAccessInterface(network.player, replacement),
                            "A replacement part is not the host tracked by the original ID");
                    assertRejectedId(helper, source, network.player, partId);
                }).thenSucceed();
    }

    @GameTest(template = "interface_network", timeoutTicks = 220)
    public static void offlineAndDifferentGridTargetsLoseAccess(GameTestHelper helper) {
        Network network = installTerminal(helper, "remote_grid");
        helper.assertTrue(
                network.cable.recolourBlock(ForgeDirection.SOUTH, AEColor.Blue, network.player),
                "Terminal cable should accept blue paint before the network forms");
        ContainerInterfaceTerminal[] source = new ContainerInterfaceTerminal[1];

        helper.startSequence()
                .thenWaitUntil("wait for remote interface network", 80, () -> assertNetworkActive(helper, network))
                .thenExecute("disconnect the previously authorized tile", () -> {
                    source[0] = terminalContainer(network);
                    assertListedHostsAccessible(helper, network, source[0]);
                    helper.assertTrue(
                            network.blockInterface.recolourBlock(ForgeDirection.NORTH, AEColor.Red, network.player),
                            "Target interface should accept red paint");

                }).thenWaitUntil("wait for target to go offline", 40, () -> {
                    assertActive(helper, network.terminal, "Source terminal must remain active");
                    assertInactive(helper, network.blockInterface.getProxy(), "Disconnected target must be offline");
                }).thenExecute("reject offline target and power its isolated grid", () -> {
                    helper.assertTrue(
                            source[0].canAccessInterface(network.player, network.partInterface),
                            "Source authorization must still work for the main-grid part");
                    helper.assertFalse(
                            source[0].canAccessInterface(network.player, network.blockInterface),
                            "A tracked but offline interface must not be accessible");
                    assertRejectedId(helper, source[0], network.player, trackedId(source[0], network.blockInterface));
                    TileEntity assembler = helper.assertTileEntityPresent("assembler");
                    helper.destroyBlock("assembler");
                    helper.assertTrue(
                            helper.getWorld().setBlock(
                                    assembler.xCoord,
                                    assembler.yCoord,
                                    assembler.zCoord,
                                    network.controller.getBlockType(),
                                    network.controller.getBlockMetadata(),
                                    3),
                            "A self-powered controller should replace the assembler on the isolated grid");
                }).thenWaitUntil("wait for isolated target grid to activate", 80, () -> {
                    assertActive(helper, network.terminal, "Source terminal must remain active");
                    assertActive(helper, network.blockInterface.getProxy(), "Target should be active on its own grid");
                    helper.assertNotSame(
                            network.terminal.getActionableNode().getGrid(),
                            network.blockInterface.getActionableNode().getGrid(),
                            "Target must be on a separate, unreachable grid");
                }).thenExecute("reject an active target on a different grid", () -> {
                    helper.assertTrue(
                            source[0].canAccessInterface(network.player, network.partInterface),
                            "Main-grid access must remain valid after the split");
                    helper.assertFalse(
                            source[0].canAccessInterface(network.player, network.blockInterface),
                            "Being active is insufficient when the target grid is unreachable");
                    assertRejectedId(helper, source[0], network.player, trackedId(source[0], network.blockInterface));
                }).thenSucceed();
    }

    @GameTest(template = "interface_network", timeoutTicks = 100)
    public static void removedTerminalRevokesSourceAccess(GameTestHelper helper) {
        Network network = installTerminal(helper, "remote_source_removal");

        helper.startSequence()
                .thenWaitUntil("wait for remote interface network", 80, () -> assertNetworkActive(helper, network))
                .thenExecute("remove the authorized source terminal", () -> {
                    ContainerInterfaceTerminal source = terminalContainer(network);
                    assertListedHostsAccessible(helper, network, source);
                    network.terminal.getHost().removePart(network.terminal.getSide(), true);

                    helper.assertFalse(
                            source.canAccessInterface(network.player, network.blockInterface),
                            "A removed terminal anchor must revoke target access");
                    helper.assertFalse(
                            source.canAccessInterface(network.player, network.partInterface),
                            "Removing the anchor must also revoke part access");
                    assertRejectedId(helper, source, network.player, trackedId(source, network.blockInterface));
                }).thenSucceed();
    }

    @GameTest(template = "interface_network", timeoutTicks = 100)
    public static void remoteSlotsUseHostInventoriesAndOriginGuardsSecondaryGui(GameTestHelper helper) {
        Network network = installTerminal(helper, "remote_slots");

        helper.startSequence()
                .thenWaitUntil("wait for remote interface network", 80, () -> assertNetworkActive(helper, network))
                .thenExecute("check real remote slots and origin invalidation", () -> {
                    ContainerInterfaceTerminal source = terminalContainer(network);
                    assertListedHostsAccessible(helper, network, source);
                    IInterfaceHost[] hosts = { network.blockInterface, network.partInterface };
                    ContainerRemoteInterface[] remotes = new ContainerRemoteInterface[hosts.length];
                    ContainerPriority[] secondary = new ContainerPriority[hosts.length];
                    ContainerInterface[] ordinary = new ContainerInterface[hosts.length];
                    RemoteInterfaceGui[] guards = new RemoteInterfaceGui[hosts.length];
                    PrimaryGui terminalGui = source.createPrimaryGui();

                    for (int i = 0; i < hosts.length; i++) {
                        IInterfaceHost host = hosts[i];
                        ContainerRemoteInterface remote = new ContainerRemoteInterface(network.player.inventory, host);
                        remotes[i] = remote;
                        helper.assertSame(host, remote.getTarget(), "Remote container must retain the actual host");
                        helper.assertTrue(
                                remote.hasRemoteInterfaceOrigin(),
                                "An unbound remote must still be classified as remote-origin");
                        helper.assertFalse(
                                remote.isValidContainer(),
                                "Server remote starts invalid before authorization");
                        helper.assertFalse(
                                remote.canInteractWith(network.player),
                                "An unbound remote must reject access");
                        assertBackingSlots(helper, network.player, host, remote);

                        ForgeDirection side = host instanceof AEBasePart p ? p.getSide() : ForgeDirection.UNKNOWN;
                        TileEntity tile = host.getTileEntity();
                        ContainerOpenContext context = new ContainerOpenContext(host);
                        context.setWorld(tile.getWorldObj());
                        context.setX(tile.xCoord);
                        context.setY(tile.yCoord);
                        context.setZ(tile.zCoord);
                        context.setSide(side);
                        remote.setOpenContext(context);
                        RemoteInterfaceGui guard = new RemoteInterfaceGui(source, host, host.getSelfRep(), side);
                        guards[i] = guard;
                        helper.assertTrue(
                                guard.canInteractWith(network.player),
                                "Origin guard must first accept valid access");
                        remote.setRemoteGui(guard, terminalGui);
                        helper.assertTrue(
                                remote.isValidContainer(),
                                "Binding an authorized origin activates the remote");
                        helper.assertTrue(
                                remote.canInteractWith(network.player),
                                "Bound remote must first allow interaction");
                        helper.assertSame(
                                guard,
                                remote.createPrimaryGui(),
                                "Secondary GUIs must inherit the origin guard");
                        helper.assertSame(
                                terminalGui,
                                remote.getPrimaryGui(),
                                "Remote return target must remain the terminal");
                        Slot icon = remote.getSlot(remote.inventorySlots.size() - 1);
                        helper.assertSame(
                                terminalGui.getIcon(),
                                icon.getStack(),
                                "Extra slot must hold the terminal icon");

                        if (i == 0) {
                            assertCrossHostRenameRejected(helper, network.player, remote, network.partInterface);
                        }

                        ordinary[i] = new ContainerInterface(network.player.inventory, host);
                        ordinary[i].setPrimaryGui(guard);
                        helper.assertTrue(
                                ordinary[i].hasRemoteInterfaceOrigin(),
                                "An ordinary interface container must retain its remote origin");
                        helper.assertTrue(
                                ordinary[i].canInteractWith(network.player),
                                "An ordinary destination must first allow the valid remote origin");

                        secondary[i] = new ContainerPriority(network.player.inventory, (IPriorityHost) host);
                        secondary[i].setPrimaryGui(remote.createPrimaryGui());
                        helper.assertTrue(
                                secondary[i].canInteractWith(network.player),
                                "Secondary GUI must first accept access");
                        helper.assertSame(
                                guard,
                                secondary[i].createPrimaryGui(),
                                "Secondary navigation must retain the same guard");
                    }

                    source.setValidContainer(false);
                    for (int i = 0; i < hosts.length; i++) {
                        helper.assertFalse(
                                source.canAccessInterface(network.player, hosts[i]),
                                "Invalid source denies host access");
                        helper.assertFalse(
                                guards[i].canInteractWith(network.player),
                                "Origin guard must reject an invalid source");
                        helper.assertFalse(
                                remotes[i].canInteractWith(network.player),
                                "Remote must reject an invalid origin");
                        remotes[i].detectAndSendChanges();
                        helper.assertFalse(remotes[i].isValidContainer(), "Remote tick must invalidate the container");
                        helper.assertFalse(
                                ordinary[i].canInteractWith(network.player),
                                "An ordinary destination must reject an invalid source");
                        helper.assertFalse(
                                secondary[i].canInteractWith(network.player),
                                "Secondary GUI must not bypass the origin");
                        secondary[i].detectAndSendChanges();
                        helper.assertFalse(
                                secondary[i].isValidContainer(),
                                "Secondary tick must invalidate the container");
                        assertRejectedId(helper, source, network.player, trackedId(source, hosts[i]));
                    }
                }).thenSucceed();
    }

    private static void assertCrossHostRenameRejected(GameTestHelper helper, FakePlayer player,
            ContainerRemoteInterface remote, PartInterface otherHost) {
        TileEntity otherTile = otherHost.getTileEntity();
        helper.assertNotSame(
                remote.getOpenContext().getTile(),
                otherTile,
                "Rename request must target a different listed host");
        Container previous = player.openContainer;
        ByteBuf data = Unpooled.buffer();
        try {
            // Use the receive-side constructor so the handler reads the requested coordinates and side.
            data.writeInt(otherTile.xCoord);
            data.writeInt(otherTile.yCoord);
            data.writeInt(otherTile.zCoord);
            data.writeInt(player.dimension);
            data.writeInt(otherHost.getSide().ordinal());
            PacketRemoteRename packet = new PacketRemoteRename(data);
            player.openContainer = remote;
            packet.serverPacketData(null, packet, player);
            helper.assertSame(remote, player.openContainer, "Cross-host rename must not open another GUI");
        } finally {
            player.openContainer = previous;
            data.release();
        }
    }

    private static Network installTerminal(GameTestHelper helper, String playerName) {
        TileCableBus cable = helper.assertTileEntityPresent(TileCableBus.class, "block_interface_cable");
        helper.assertEquals(
                ForgeDirection.UP,
                cable.addPart(
                        AEApi.instance().definitions().parts().interfaceTerminal().maybeStack(1).get(),
                        ForgeDirection.UP,
                        null),
                "Interface terminal should fit the cable bus above the block interface");
        return new Network(
                helper.assertTileEntityPresent(TileController.class, "controller"),
                cable,
                part(helper, "block_interface_cable", PartInterfaceTerminal.class),
                helper.assertTileEntityPresent(TileInterface.class, "block_interface"),
                part(helper, "part_interface_host", PartInterface.class),
                helper.spawnFakePlayer(playerName));
    }

    private static void assertNetworkActive(GameTestHelper helper, Network network) {
        assertActive(helper, network.controller.getProxy(), "Controller must be active");
        assertActive(helper, network.terminal, "Installed interface terminal must be active");
        assertActive(helper, network.blockInterface.getProxy(), "Block interface must be active");
        assertActive(helper, network.partInterface, "Part interface must be active");
    }

    private static ContainerInterfaceTerminal terminalContainer(Network network) {
        ContainerInterfaceTerminal source = new ContainerInterfaceTerminal(network.player.inventory, network.terminal);
        TileEntity tile = network.terminal.getTile();
        ContainerOpenContext context = new ContainerOpenContext(network.terminal);
        context.setWorld(tile.getWorldObj());
        context.setX(tile.xCoord);
        context.setY(tile.yCoord);
        context.setZ(tile.zCoord);
        context.setSide(network.terminal.getSide());
        source.setOpenContext(context);
        return source;
    }

    private static void assertListedHostsAccessible(GameTestHelper helper, Network network,
            ContainerInterfaceTerminal source) {
        helper.assertTrue(
                source.canAccessInterface(network.player, network.blockInterface),
                "Listed tile must be authorized");
        helper.assertTrue(
                source.canAccessInterface(network.player, network.partInterface),
                "Listed part must be authorized");
    }

    private static void assertRejectedId(GameTestHelper helper, ContainerInterfaceTerminal source, FakePlayer player,
            long id) {
        Container previous = player.openContainer;
        try {
            // Only negative opens are exercised: FakePlayer has no real FML GUI/network connection.
            player.openContainer = source;
            source.openInterface(player, id);
            helper.assertSame(source, player.openContainer, "Rejected ID must not open another GUI");
        } finally {
            player.openContainer = previous;
        }
    }

    private static long trackedId(ContainerInterfaceTerminal source, IInterfaceHost host) {
        // Read constructor-populated IDs without exposing production internals or sending terminal updates.
        try {
            Field byId = ContainerInterfaceTerminal.class.getDeclaredField("trackedById");
            byId.setAccessible(true);
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) byId.get(source)).entrySet()) {
                Field machine = entry.getValue().getClass().getDeclaredField("machine");
                machine.setAccessible(true);
                if (machine.get(entry.getValue()) == host) return (Long) entry.getKey();
            }
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Could not read terminal's tracked IDs", e);
        }
        throw new AssertionError("Expected interface host to have a constructor-populated terminal ID");
    }

    private static void assertBackingSlots(GameTestHelper helper, FakePlayer player, IInterfaceHost host,
            ContainerRemoteInterface remote) {
        ContainerInterface local = new ContainerInterface(player.inventory, host);
        helper.assertEquals(
                local.inventorySlots.size() + 1,
                remote.inventorySlots.size(),
                "Remote adds only an icon slot");
        IInventory upgrades = host.getInterfaceDuality().getUpgrades();
        IInventory patterns = host.getInterfaceDuality().getPatterns();
        helper.assertEquals(
                4,
                backingSlotCount(remote, upgrades),
                "All four upgrade slots must use the host inventory");
        helper.assertEquals(
                patterns.getSizeInventory(),
                backingSlotCount(remote, patterns),
                "All pattern slots must use the host inventory");
        Slot upgrade = backingSlot(remote, upgrades, 0);
        Slot pattern = backingSlot(remote, patterns, 0);
        ItemStack card = AEApi.instance().definitions().materials().cardCrafting().maybeStack(1).get();
        ItemStack encoded = processingPattern();
        helper.assertTrue(upgrade.isItemValid(card), "Remote upgrade slot must accept a real interface upgrade");
        helper.assertTrue(pattern.isItemValid(encoded), "Remote pattern slot must accept an encoded pattern");
        upgrade.putStack(card);
        pattern.putStack(encoded);
        helper.assertSame(card, upgrades.getStackInSlot(0), "Upgrade slot writes must reach the host");
        helper.assertSame(encoded, patterns.getStackInSlot(0), "Pattern slot writes must reach the host");
        InventoryHelper.setSlot(upgrades, 0, card.copy());
        InventoryHelper.setSlot(patterns, 0, encoded.copy());
        helper.assertSame(upgrades.getStackInSlot(0), upgrade.getStack(), "Remote upgrade slot must read host changes");
        helper.assertSame(patterns.getStackInSlot(0), pattern.getStack(), "Remote pattern slot must read host changes");
        Slot icon = remote.getSlot(remote.inventorySlots.size() - 1);
        helper.assertTrue(icon instanceof SlotInaccessible, "The only extra slot must be inaccessible");
        helper.assertFalse(icon.isItemValid(card), "Icon slot must reject insertion");
        helper.assertFalse(icon.canTakeStack(player), "Icon slot must reject extraction");
    }

    private static int backingSlotCount(Container container, IInventory inventory) {
        int count = 0;
        for (Object entry : container.inventorySlots) {
            if (((Slot) entry).inventory == inventory) count++;
        }
        return count;
    }

    private static Slot backingSlot(Container container, IInventory inventory, int index) {
        for (Object entry : container.inventorySlots) {
            Slot slot = (Slot) entry;
            if (slot.inventory == inventory && slot.getSlotIndex() == index) return slot;
        }
        throw new AssertionError("Missing host-backed container slot " + index);
    }

    private static ItemStack processingPattern() {
        ItemStack pattern = AEApi.instance().definitions().items().encodedPattern().maybeStack(1).get();
        NBTTagCompound tags = new NBTTagCompound();
        NBTTagList inputs = new NBTTagList();
        NBTTagList outputs = new NBTTagList();
        inputs.appendTag(new ItemStack(Blocks.cobblestone).writeToNBT(new NBTTagCompound()));
        outputs.appendTag(new ItemStack(Blocks.stone).writeToNBT(new NBTTagCompound()));
        tags.setBoolean("crafting", false);
        tags.setTag("in", inputs);
        tags.setTag("out", outputs);
        pattern.setTagCompound(tags);
        return pattern;
    }

    @Desugar
    private record Network(TileController controller, TileCableBus cable, PartInterfaceTerminal terminal,
            TileInterface blockInterface, PartInterface partInterface, FakePlayer player) {}
}
