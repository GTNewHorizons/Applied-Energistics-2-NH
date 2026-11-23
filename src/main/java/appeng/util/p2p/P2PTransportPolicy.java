package appeng.util.p2p;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class P2PTransportPolicy {

    public static final P2PTransportPolicy ONE_WAY_PUSH = new P2PTransportPolicy(
            new P2PSideAccess(P2PRoute.OUTPUTS, P2PRoute.NONE),
            P2PSideAccess.CLOSED);
    public static final P2PTransportPolicy ONE_WAY_PUSH_PULL = new P2PTransportPolicy(
            new P2PSideAccess(P2PRoute.OUTPUTS, P2PRoute.NONE),
            new P2PSideAccess(P2PRoute.NONE, P2PRoute.INPUT));
    public static final P2PTransportPolicy TWO_WAY_PUSH = new P2PTransportPolicy(
            new P2PSideAccess(P2PRoute.OUTPUTS, P2PRoute.NONE),
            new P2PSideAccess(P2PRoute.PEERS, P2PRoute.NONE));
    public static final P2PTransportPolicy TWO_WAY_PUSH_PULL = new P2PTransportPolicy(
            new P2PSideAccess(P2PRoute.OUTPUTS, P2PRoute.OUTPUTS),
            new P2PSideAccess(P2PRoute.PEERS, P2PRoute.PEERS));

    private final P2PSideAccess inputSide;
    private final P2PSideAccess outputSide;

    public P2PTransportPolicy(P2PSideAccess inputSide, P2PSideAccess outputSide) {
        this.inputSide = inputSide;
        this.outputSide = outputSide;

        if (inputSide.uses(P2PRoute.INPUT) || inputSide.uses(P2PRoute.PEERS)) {
            throw new IllegalArgumentException("Invalid transport policy");
        }
        if (outputSide.uses(P2PRoute.OUTPUTS)) {
            throw new IllegalArgumentException("Invalid transport policy");
        }
    }

    public P2PSideAccess inputSide() {
        return inputSide;
    }

    public P2PSideAccess outputSide() {
        return outputSide;
    }

    public P2PSideAccess side(boolean isOutput) {
        return isOutput ? outputSide : inputSide;
    }

    public boolean inputReachesOutputs() {
        return inputSide.uses(P2PRoute.OUTPUTS);
    }

    public boolean outputReachesInput() {
        return outputSide.uses(P2PRoute.INPUT) || outputSide.uses(P2PRoute.PEERS);
    }

    public boolean outputReachesPeers() {
        return outputSide.uses(P2PRoute.PEERS);
    }
}
