package com.projecturanus.betterp2p.client.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import com.projecturanus.betterp2p.BetterP2P;

/**
 * Parses the search query into filters.
 * <ul>
 * <li>{@code @in} / {@code @out}: inputs / outputs</li>
 * <li>{@code @b} / {@code @u}: bound / unbound tunnels</li>
 * <li>{@code @type=<type1>;<type2>;...}: tunnel types</li>
 * <li>anything else: tunnel name</li>
 * </ul>
 */
public class InfoFilter {

    // https://stackoverflow.com/questions/366202/regex-for-splitting-a-string-using-space-when-not-surrounded-by-single-or-double
    private static final Pattern SEARCH_REGEX = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");

    private static final Filter[] FLAG_FILTERS = { Filter.INPUT, Filter.OUTPUT, Filter.BOUND, Filter.UNBOUND };

    private final Map<Filter, @Nullable List<String>> activeFilters = new EnumMap<>(Filter.class);

    public Map<Filter, @Nullable List<String>> getActiveFilters() {
        return activeFilters;
    }

    public void updateFilter(String query) {
        activeFilters.clear();
        Matcher matcher = SEARCH_REGEX.matcher(query);
        while (matcher.find()) {
            String token = matcher.group();
            if (token.startsWith("@")) {
                updateTagFilter(token);
                continue;
            }
            List<String> names = new ArrayList<>();
            activeFilters.putIfAbsent(Filter.NAME, names);
            if (matcher.group(1) != null) {
                names.add(matcher.group(1));
            } else if (matcher.group(2) != null) {
                names.add(matcher.group(2));
            } else {
                activeFilters.get(Filter.NAME).add(token);
            }
        }
    }

    private void updateTagFilter(String token) {
        for (Filter filter : FLAG_FILTERS) {
            if (filter.pattern.matcher(token).matches()) {
                activeFilters.putIfAbsent(filter, null);
                return;
            }
        }
        Matcher typeMatcher = Filter.TYPE.pattern.matcher(token);
        if (typeMatcher.matches()) {
            List<String> types = new ArrayList<>();
            activeFilters.putIfAbsent(Filter.TYPE, types);
            Collections.addAll(types, typeMatcher.group(1).split(";", -1));
        }
    }

    private static String removeSurroundingQuotes(String s) {
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    public enum Filter {

        INPUT("\\A@in\\z", (info, args) -> !info.output),
        OUTPUT("\\A@out\\z", (info, args) -> info.output),
        BOUND("\\A@b\\z", (info, args) -> info.getFrequency() != 0L),
        UNBOUND("\\A@u\\z", (info, args) -> info.getFrequency() == 0L || info.isError()),
        TYPE("\\A@types*=(.+)\\z", (info, args) -> {
            String tags = BetterP2P.proxy.getP2PFromIndex(info.type).dispName.toLowerCase(Locale.ROOT);
            for (String f : args) {
                if (tags.contains(f.toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
            return false;
        }),
        NAME("\"?.+\"?", (info, args) -> {
            String name = info.name.toLowerCase(Locale.ROOT);
            for (String f : args) {
                if (name.contains(removeSurroundingQuotes(f))) {
                    return true;
                }
            }
            return false;
        });

        private final Pattern pattern;
        private final BiPredicate<InfoWrapper, List<String>> filter;

        Filter(String pattern, BiPredicate<InfoWrapper, List<String>> filter) {
            this.pattern = Pattern.compile(pattern);
            this.filter = filter;
        }

        public boolean test(InfoWrapper info, @Nullable List<String> args) {
            return filter.test(info, args);
        }
    }
}
