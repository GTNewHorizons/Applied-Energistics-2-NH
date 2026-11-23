package com.projecturanus.betterp2p.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.projecturanus.betterp2p.network.data.P2PLocation;

public final class ClientCache {

    private static final List<P2PLocation> positions = new ArrayList<>();
    private static @Nullable P2PLocation selectedPosition;
    private static String searchText = "";

    private ClientCache() {}

    public static List<P2PLocation> getPositions() {
        return positions;
    }

    public static void setPositions(Collection<P2PLocation> newPositions) {
        positions.clear();
        positions.addAll(newPositions);
    }

    public static @Nullable P2PLocation getSelectedPosition() {
        return selectedPosition;
    }

    public static void setSelectedPosition(@Nullable P2PLocation position) {
        selectedPosition = position;
    }

    public static String getSearchText() {
        return searchText;
    }

    public static void setSearchText(String text) {
        searchText = text;
    }

    public static void clear() {
        positions.clear();
        selectedPosition = null;
    }
}
