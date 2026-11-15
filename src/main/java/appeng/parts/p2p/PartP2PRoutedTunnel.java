package appeng.parts.p2p;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import appeng.me.GridAccessException;
import appeng.util.p2p.P2PRoute;
import appeng.util.p2p.P2PSideAccess;
import appeng.util.p2p.P2PTransportPolicy;

public abstract class PartP2PRoutedTunnel<T extends PartP2PRoutedTunnel<T>> extends PartP2PTunnelNormal<T> {

    public PartP2PRoutedTunnel(final ItemStack is) {
        super(is);
    }

    public abstract P2PTransportPolicy getTransportPolicy();

    protected final P2PSideAccess getSideAccess() {
        return getTransportPolicy().side(this.isOutput());
    }

    protected final List<T> getInsertTargets() {
        return this.resolveRoute(getSideAccess().insert());
    }

    protected final List<T> getExtractTargets() {
        return this.resolveRoute(getSideAccess().extract());
    }

    protected final List<T> getDependentTunnels() {
        final P2PTransportPolicy policy = this.getTransportPolicy();
        final List<T> tunnels = new ArrayList<>();

        if (this.isOutput()) {
            if (policy.outputReachesPeers()) {
                this.addOtherOutputs(tunnels);
            }
            if (policy.inputReachesOutputs()) {
                this.addInput(tunnels);
            }
        } else if (policy.outputReachesInput()) {
            this.addOtherOutputs(tunnels);
        }

        return tunnels;
    }

    protected final TileEntity getFacingTileEntity() {
        return this.getTile().getWorldObj().getTileEntity(
                this.getTile().xCoord + this.getSide().offsetX,
                this.getTile().yCoord + this.getSide().offsetY,
                this.getTile().zCoord + this.getSide().offsetZ);
    }

    private List<T> resolveRoute(final P2PRoute route) {
        return switch (route) {
            case OUTPUTS -> this.addOtherOutputs(new ArrayList<>());
            case INPUT -> this.addInput(new ArrayList<>(1));
            case PEERS -> this.addInput(this.addOtherOutputs(new ArrayList<>()));
            default -> Collections.emptyList();
        };
    }

    protected final List<T> getPeerTunnels() {
        return this.resolveRoute(P2PRoute.PEERS);
    }

    private List<T> addOtherOutputs(final List<T> into) {
        try {
            for (final T output : this.getOutputs()) {
                if (output != this) {
                    into.add(output);
                }
            }
        } catch (final GridAccessException ignored) {
            // :P
        }
        return into;
    }

    private List<T> addInput(final List<T> into) {
        final T input = this.getInput();
        if (input != null && input != this) {
            into.add(input);
        }
        return into;
    }
}
