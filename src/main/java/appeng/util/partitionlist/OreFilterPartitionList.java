package appeng.util.partitionlist;

import java.util.Collections;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.oredict.OreDictionary;

import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.core.AELog;
import codechicken.nei.FormattedTextField.TextFormatter;
import cpw.mods.fml.common.Optional;

public class OreFilterPartitionList implements IPartitionList {

    private static final Pattern BOOLEAN_OPERAND_PATTERN = Pattern.compile("[^&|]+");

    @Optional.Interface(modid = "NotEnoughItems", iface = "codechicken.nei.FormattedTextField.TextFormatter")
    public static class OreFilterTextFormatter implements TextFormatter {

        public String format(String text) {

            if (notAWildcard(text)) {
                return text.replaceAll(
                        "([" + Pattern.quote("^$+()[].*?|:{}\\") + "])",
                        EnumChatFormatting.AQUA + "$1" + EnumChatFormatting.RESET);
            } else if (!text.isEmpty()) {
                final String[] parts = text.split("\\|");
                StringJoiner formattedText = new StringJoiner(EnumChatFormatting.GRAY + "|" + EnumChatFormatting.RESET);

                for (String filterText : parts) {
                    formattedText.add(formatRules(filterText));
                }

                if (text.endsWith("|")) {
                    formattedText.add("");
                }

                return formattedText.toString();
            }

            return text;
        }

        private String formatRules(String text) {
            final String[] parts = text.split("\\&");
            StringJoiner formattedText = new StringJoiner(EnumChatFormatting.GRAY + "&" + EnumChatFormatting.RESET);

            for (String filterText : parts) {
                formattedText.add(formatPattern(filterText));
            }

            if (text.endsWith("&")) {
                formattedText.add("");
            }

            return formattedText.toString();
        }

        private String formatPattern(String filter) {
            final Matcher filterMatcher = Pattern.compile("(\\s*)(!*)(.*)").matcher(filter);

            if (filterMatcher.find()) {
                StringBuilder formattedPart = new StringBuilder();
                formattedPart.append(filterMatcher.group(1));

                if ("!".equals(filterMatcher.group(2))) {
                    formattedPart.append(EnumChatFormatting.RED + "!" + EnumChatFormatting.RESET);
                }

                formattedPart.append(
                        filterMatcher.group(3).replace("*", EnumChatFormatting.AQUA + "*" + EnumChatFormatting.RESET));
                return formattedPart.toString();
            }

            return filter;
        }
    }

    private final @Nullable Predicate<AEItemKey> filterPredicate;

    public OreFilterPartitionList(String filter) {
        filterPredicate = makeFilter(filter.trim());
    }

    @Override
    public boolean isListed(final AEKey input) {
        return this.filterPredicate != null && input instanceof AEItemKey itemKey && this.filterPredicate.test(itemKey);
    }

    @Override
    public boolean isEmpty() {
        return this.filterPredicate == null;
    }

    @Override
    public Iterable<AEKey> getKeys() {
        return Collections.emptyList();
    }

    public static @Nullable Predicate<AEItemKey> makeFilter(String f) {
        try {
            Predicate<ItemStack> matcher = makeMatcher(f);
            if (matcher == null) return null;
            return new OreListMatcher(matcher);
        } catch (Exception ex) {
            AELog.debug(ex);
            return null;
        }
    }

    private static Predicate<ItemStack> makeMatcher(String f) {
        Predicate<ItemStack> matcher = null;
        if (notAWildcard(f)) {
            final Predicate<String> test = Pattern.compile(f).asPredicate();
            matcher = (is) -> is != null
                    && IntStream.of(OreDictionary.getOreIDs(is)).mapToObj(OreDictionary::getOreName).anyMatch(test);
        } else if (!f.isEmpty()) {
            matcher = makeBooleanMatcher(f, OreFilterPartitionList::filterToItemStackPredicate);
        }
        return matcher;
    }

    static <T> Predicate<T> makeBooleanMatcher(String f, Function<String, Predicate<T>> predicateFactory) {
        Predicate<T> matcher = null;
        final Matcher operands = BOOLEAN_OPERAND_PATTERN.matcher(f);
        int endLast = 0;

        while (operands.find()) {
            String filter = operands.group().trim();
            if (filter.isEmpty()) continue;
            boolean negated = filter.startsWith("!");
            if (negated) filter = filter.substring(1);

            Predicate<T> test = predicateFactory.apply(filter);

            if (negated) test = test.negate();

            if (matcher == null) {
                matcher = test;
            } else {
                boolean or = f.substring(endLast, operands.start()).contains("|");
                matcher = or ? matcher.or(test) : matcher.and(test);
            }

            endLast = operands.end();
        }
        return matcher;
    }

    private static class OreListMatcher implements Predicate<AEItemKey> {

        final Map<ItemRef, Boolean> cache = new ConcurrentHashMap<>();
        final Predicate<ItemStack> matcher;

        public OreListMatcher(Predicate<ItemStack> matcher) {
            this.matcher = matcher;
        }

        public boolean test(@Nullable AEItemKey t) {
            if (t == null) return false;
            return this.cache.computeIfAbsent(new ItemRef(t), ref -> this.matcher.test(t.getReadonlyStack()));
        }
    }

    private static boolean notAWildcard(String f) {
        return f.contains("\\") || f.contains("^")
                || f.contains("$")
                || f.contains("+")
                || f.contains("(")
                || f.contains(")")
                || f.contains("[")
                || f.contains("]");
    }

    private static class ItemRef {

        private final Item ref;
        private final int damage;
        private final int hash;

        ItemRef(final AEItemKey stack) {
            this.ref = stack.getItem();
            this.damage = stack.getItem().isDamageable() ? 0 : stack.getItemDamage();
            this.hash = this.ref.hashCode() ^ this.damage;
        }

        @Override
        public int hashCode() {
            return this.hash;
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == null || this.getClass() != obj.getClass()) return false;
            final ItemRef other = (ItemRef) obj;
            return this.damage == other.damage && this.ref == other.ref;
        }

        @Override
        public String toString() {
            return "ItemRef [ref=" + this.ref
                    .getUnlocalizedName() + ", damage=" + this.damage + ", hash=" + this.hash + ']';
        }
    }

    private static Predicate<ItemStack> filterToItemStackPredicate(String filter) {
        final Predicate<String> test = filterToPredicate(filter);
        return (is) -> is != null
                && IntStream.of(OreDictionary.getOreIDs(is)).mapToObj(OreDictionary::getOreName).anyMatch(test);
    }

    private static Predicate<String> filterToPredicate(String filter) {
        int numStars = StringUtils.countMatches(filter, "*");
        if (numStars == filter.length()) {
            return (str) -> true;
        } else if (filter.length() > 2 && filter.startsWith("*") && filter.endsWith("*") && numStars == 2) {
            final String pattern = filter.substring(1, filter.length() - 1);
            return (str) -> str.contains(pattern);
        } else if (filter.length() >= 2 && filter.startsWith("*") && numStars == 1) {
            final String pattern = filter.substring(1);
            return (str) -> str.endsWith(pattern);
        } else if (filter.length() >= 2 && filter.endsWith("*") && numStars == 1) {
            final String pattern = filter.substring(0, filter.length() - 1);
            return (str) -> str.startsWith(pattern);
        } else if (numStars == 0) {
            return (str) -> str.equals(filter);
        } else {
            String filterRegexFragment = filter.replace("*", ".*");
            String regexPattern = "^" + filterRegexFragment + "$";
            final Pattern pattern = Pattern.compile(regexPattern);
            return pattern.asPredicate();
        }
    }
}
