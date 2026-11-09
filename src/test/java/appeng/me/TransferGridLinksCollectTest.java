package appeng.me;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public final class TransferGridLinksCollectTest {

    private final Map<String, Collection<String>> graph = new HashMap<>();

    private void link(final String from, final String... to) {
        this.graph.put(from, Arrays.asList(to));
    }

    private List<String> collect(final String start, final int depth) {
        return TransferGridLinks.collect(start, g -> this.graph.getOrDefault(g, Collections.emptyList()), depth);
    }

    @Test
    public void startsWithTheGivenNode() {
        assertEquals(Collections.singletonList("a"), this.collect("a", 10));
    }

    @Test
    public void visitsNearerNodesBeforeFartherOnes() {
        this.link("a", "b", "c");
        this.link("b", "d");
        this.link("c", "e");

        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), this.collect("a", 10));
    }

    @Test
    public void visitsEachNodeOnceInCycles() {
        this.link("a", "b");
        this.link("b", "c", "a");
        this.link("c", "a", "b");

        assertEquals(Arrays.asList("a", "b", "c"), this.collect("a", 10));
    }

    @Test
    public void depthLimitsHowManyLinksAreFollowed() {
        this.link("a", "b");
        this.link("b", "c");
        this.link("c", "d");

        assertEquals(Collections.singletonList("a"), this.collect("a", 0));
        assertEquals(Arrays.asList("a", "b", "c"), this.collect("a", 2));
    }

    @Test
    public void ignoresMissingLinks() {
        this.link("a", null, "b");

        assertEquals(Arrays.asList("a", "b"), this.collect("a", 10));
    }
}
