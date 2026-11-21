package com.projecturanus.betterp2p.client.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.projecturanus.betterp2p.client.gui.InfoFilter.Filter;
import com.projecturanus.betterp2p.client.gui.widget.WidgetScrollBar;
import com.projecturanus.betterp2p.network.data.P2PLocation;

public class InfoList {

    private final Map<P2PLocation, InfoWrapper> masterMap = new HashMap<>();
    private final List<InfoWrapper> sorted = new ArrayList<>();
    private List<InfoWrapper> filtered = Collections.emptyList();
    private final InfoFilter filter = new InfoFilter();
    private final Supplier<String> search;
    private @Nullable P2PLocation selectedEntry;

    public InfoList(Collection<InfoWrapper> initList, Supplier<String> search) {
        this.search = search;
        for (InfoWrapper info : initList) {
            masterMap.put(info.loc, info);
        }
    }

    public List<InfoWrapper> getSorted() {
        return sorted;
    }

    public List<InfoWrapper> getFiltered() {
        return filtered;
    }

    public @Nullable P2PLocation getSelectedEntry() {
        return selectedEntry;
    }

    public @Nullable InfoWrapper getSelectedInfo() {
        return selectedEntry == null ? null : masterMap.get(selectedEntry);
    }

    public int size() {
        return masterMap.size();
    }

    private void resort() {
        InfoWrapper selected = getSelectedInfo();
        sorted.sort(Comparator.comparingLong(info -> {
            if (info.loc.equals(selectedEntry)) {
                return -2;
            }
            if (isSameFrequency(info, selected)) {
                return info.output ? -1 : -3;
            }
            return info.getFrequency() + Short.MAX_VALUE;
        }));
    }

    public void refilter() {
        filter.updateFilter(search.get().toLowerCase(Locale.ROOT));
        Map<Filter, @Nullable List<String>> activeFilters = filter.getActiveFilters();

        List<InfoWrapper> result = new ArrayList<>();
        for (InfoWrapper info : sorted) {
            if (info.loc.equals(selectedEntry) || matchesAll(info, activeFilters)) {
                result.add(info);
            }
        }

        InfoWrapper selected = getSelectedInfo();
        List<String> names = activeFilters.get(Filter.NAME);
        result.sort(Comparator.comparingLong(info -> filteredSortKey(info, selected, names)));
        filtered = result;
    }

    private static boolean matchesAll(InfoWrapper info, Map<Filter, @Nullable List<String>> activeFilters) {
        for (Map.Entry<Filter, @Nullable List<String>> entry : activeFilters.entrySet()) {
            if (!entry.getKey().test(info, entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    private long filteredSortKey(InfoWrapper info, @Nullable InfoWrapper selected, @Nullable List<String> names) {
        if (info.loc.equals(selectedEntry)) {
            return Long.MIN_VALUE + 1;
        }
        if (isSameFrequency(info, selected)) {
            return info.output ? Long.MIN_VALUE + 2 : Long.MIN_VALUE;
        }
        if (names != null) {
            long hits = 0;
            String name = info.name;
            for (String f : names) {
                int index = indexOfIgnoreCase(name, f);
                if (index >= 0) {
                    hits++;
                    name = name.substring(0, index) + name.substring(index + f.length());
                }
            }
            return -(hits * hits) + name.length();
        }
        return info.getFrequency() + Short.MAX_VALUE - (info.output ? 0 : 1);
    }

    private static boolean isSameFrequency(InfoWrapper info, @Nullable InfoWrapper selected) {
        return info.getFrequency() != 0L && selected != null && info.getFrequency() == selected.getFrequency();
    }

    private static int indexOfIgnoreCase(String s, String target) {
        for (int i = 0; i + target.length() <= s.length(); i++) {
            if (s.regionMatches(true, i, target, 0, target.length())) {
                return i;
            }
        }
        return -1;
    }

    public void refresh() {
        sorted.clear();
        sorted.addAll(masterMap.values());
        resort();
        refilter();
    }

    public void rebuild(Collection<InfoWrapper> updateList, WidgetScrollBar scrollbar, int numEntries) {
        masterMap.clear();
        update(updateList, scrollbar, numEntries);
    }

    public void update(Collection<InfoWrapper> updateList, WidgetScrollBar scrollbar, int numEntries) {
        for (InfoWrapper info : updateList) {
            masterMap.put(info.loc, info);
        }
        refresh();
        scrollbar.setRange(0, Math.max(0, masterMap.size() - numEntries));
    }

    public void select(@Nullable P2PLocation which) {
        InfoWrapper info = which == null ? null : masterMap.get(which);
        selectedEntry = info != null ? info.loc : null;
    }
}
