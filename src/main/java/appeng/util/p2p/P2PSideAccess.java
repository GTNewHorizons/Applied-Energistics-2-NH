package appeng.util.p2p;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class P2PSideAccess {

    private final P2PRoute insert;
    private final P2PRoute extract;

    public P2PSideAccess(P2PRoute insert, P2PRoute extract) {
        this.insert = insert;
        this.extract = extract;
    }

    public P2PRoute insert() {
        return insert;
    }

    public P2PRoute extract() {
        return extract;
    }

    public static final P2PSideAccess CLOSED = new P2PSideAccess(P2PRoute.NONE, P2PRoute.NONE);

    public boolean uses(P2PRoute route) {
        return insert == route || extract == route;
    }
}
