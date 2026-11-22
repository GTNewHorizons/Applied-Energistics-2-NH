package appeng.parts.p2p;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;

import org.jspecify.annotations.NullMarked;

import appeng.me.GridAccessException;
import appeng.util.p2p.P2PRoute;
import appeng.util.p2p.P2PSideAccess;
import appeng.util.p2p.P2PTransportPolicy;

@NullMarked
public abstract class PartP2PTunnelRouted<T extends PartP2PTunnelRouted<T>> extends PartP2PTunnelNormal<T> {

    public PartP2PTunnelRouted(final ItemStack is) {
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

    private List<T> resolveRoute(final P2PRoute route) {
        if (!this.getProxy().isActive()) {
            return Collections.emptyList();
        }
        return switch (route) {
            case OUTPUTS -> this.addOtherOutputs(new ArrayList<>());
            case INPUT -> this.addInput(new ArrayList<>(1));
            case PEERS -> this.addInput(this.addOtherOutputs(new ArrayList<>()));
            default -> Collections.emptyList();
        };
    }

    private List<T> addOtherOutputs(final List<T> into) {
        try {
            for (final T output : this.getOutputs()) {
                if (output != this && output.getProxy().isActive()) {
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
        if (input != null && input != this && input.getProxy().isActive()) {
            into.add(input);
        }
        return into;
    }
}
