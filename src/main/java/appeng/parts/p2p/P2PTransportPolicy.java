package appeng.parts.p2p;

public class P2PTransportPolicy {

    public enum Route {
        NONE,
        INPUT,
        OUTPUTS,
        PEERS
    }

    public static class SideAccess {

        private final Route insert;
        private final Route extract;

        public SideAccess(Route insert, Route extract) {
            this.insert = insert;
            this.extract = extract;
        }

        public Route insert() {
            return insert;
        }

        public Route extract() {
            return extract;
        }

        public static final SideAccess CLOSED = new SideAccess(Route.NONE, Route.NONE);

        public boolean uses(Route route) {
            return insert == route || extract == route;
        }
    }

    public static final P2PTransportPolicy ONE_WAY_PUSH = new P2PTransportPolicy(new SideAccess(Route.OUTPUTS, Route.NONE), SideAccess.CLOSED);
    public static final P2PTransportPolicy ONE_WAY_PUSH_PULL = new P2PTransportPolicy(new SideAccess(Route.OUTPUTS, Route.NONE), new SideAccess(Route.NONE, Route.INPUT));
    public static final P2PTransportPolicy TWO_WAY_PUSH = new P2PTransportPolicy(new SideAccess(Route.OUTPUTS, Route.NONE), new SideAccess(Route.PEERS, Route.NONE));
    public static final P2PTransportPolicy TWO_WAY_PUSH_PULL = new P2PTransportPolicy(new SideAccess(Route.OUTPUTS, Route.OUTPUTS), new SideAccess(Route.PEERS, Route.PEERS));

    private final SideAccess inputSide;
    private final SideAccess outputSide;

    public P2PTransportPolicy(SideAccess inputSide, SideAccess outputSide) {
        this.inputSide = inputSide;
        this.outputSide = outputSide;

        if (inputSide.uses(Route.INPUT) || inputSide.uses(Route.PEERS)) {
            throw new IllegalArgumentException("Invalid transport policy");
        }
        if (outputSide.uses(Route.OUTPUTS)) {
            throw new IllegalArgumentException("Invalid transport policy");
        }
    }

    public SideAccess inputSide() {
        return inputSide;
    }

    public SideAccess outputSide() {
        return outputSide;
    }

    public SideAccess side(boolean isOutput) {
        return isOutput ? outputSide : inputSide;
    }

    public boolean inputReachesOutputs() {
        return inputSide.uses(Route.OUTPUTS);
    }

    public boolean outputReachesInput() {
        return outputSide.uses(Route.INPUT) || outputSide.uses(Route.PEERS);
    }

    public boolean outputReachesOutputs() {
        return outputSide.uses(Route.PEERS);
    }
}
