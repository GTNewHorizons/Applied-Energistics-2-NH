package appeng.me.storage;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import appeng.api.networking.IGrid;

public final class InjectTraceDeniedTest {

    private static IGrid grid() {
        return (IGrid) Proxy.newProxyInstance(
                IGrid.class.getClassLoader(),
                new Class<?>[] { IGrid.class },
                (proxy, method, args) -> {
                    if ("equals".equals(method.getName())) {
                        return proxy == args[0];
                    }
                    if ("hashCode".equals(method.getName())) {
                        return System.identityHashCode(proxy);
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    @Test
    public void aRefusalOnAShownNetworkCounts() {
        final IGrid own = grid();
        final InjectTrace trace = new InjectTrace();
        trace.denied(own);

        assertTrue(trace.wasDeniedOnAny(Collections.singletonList(own)));
    }

    @Test
    public void aRefusalOnAHiddenNetworkDoesNotCount() {
        final IGrid own = grid();
        final IGrid shown = grid();
        final IGrid hidden = grid();
        final InjectTrace trace = new InjectTrace();
        trace.denied(hidden);

        assertFalse(trace.wasDeniedOnAny(Arrays.asList(own, shown)));
    }

    @Test
    public void nothingRefusedCountsAsNoRefusal() {
        assertFalse(new InjectTrace().wasDeniedOnAny(Collections.singletonList(grid())));
    }
}
