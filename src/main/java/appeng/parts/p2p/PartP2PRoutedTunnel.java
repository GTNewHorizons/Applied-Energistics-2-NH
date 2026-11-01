package appeng.parts.p2p;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;

import appeng.me.GridAccessException;

public abstract class PartP2PRoutedTunnel<T extends PartP2PRoutedTunnel<T>> extends PartP2PTunnelNormal<T> {

    public PartP2PRoutedTunnel(final ItemStack is) {
        super(is);
    }

    protected abstract P2PTransportPolicy getTransportPolicy();

    protected final P2PTransportPolicy.SideAccess getSideAccess() {
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
            if (policy.outputReachesOutputs()) {
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

    protected void onRouteTargetsChanged() {}

    @Override
    public void onTunnelNetworkChange() {
        super.onTunnelNetworkChange();
        for (final T tunnel : getDependentTunnels()) {
            tunnel.onRouteTargetsChanged();
        }
    }

    @Override
    public void onNeighborChanged() {
        super.onNeighborChanged();
        for (final T tunnel : getDependentTunnels()) {
            tunnel.onRouteTargetsChanged();
        }
    }

    @Override
    public void setFrequency(final long freq) {
        final List<T> oldDependents = this.getDependentTunnels();
        super.setFrequency(freq);
        for (final T tunnel : oldDependents) {
            tunnel.onRouteTargetsChanged();
        }
    }

    private List<T> resolveRoute(final P2PTransportPolicy.Route route) {
        return switch (route) {
            case OUTPUTS -> this.addOtherOutputs(new ArrayList<>());
            case INPUT -> this.addInput(new ArrayList<>(1));
            case PEERS -> this.addInput(this.addOtherOutputs(new ArrayList<>()));
            default -> Collections.emptyList();
        };
    }

    protected final List<T> getPeerTunnels() {
        return this.resolveRoute(P2PTransportPolicy.Route.PEERS);
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
