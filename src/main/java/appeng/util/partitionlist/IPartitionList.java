/*
 * This file is part of Applied Energistics 2. Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved. Applied
 * Energistics 2 is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version. Applied Energistics 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details. You should have received a copy of the GNU Lesser General Public License along with
 * Applied Energistics 2. If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.util.partitionlist;

import org.jspecify.annotations.Nullable;

import appeng.api.config.FuzzyMode;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;

public interface IPartitionList {

    boolean isListed(AEKey input);

    boolean isEmpty();

    Iterable<AEKey> getKeys();

    static Builder builder() {
        return new Builder();
    }

    final class Builder {

        // Keeps the insertion order, so that getItems lists the keys in the order of the config slots
        private final ObjectLinkedOpenHashSet<AEKey> keys = new ObjectLinkedOpenHashSet<>();
        @Nullable
        private FuzzyMode fuzzyMode;

        private Builder() {}

        public Builder add(@Nullable AEKey key) {
            if (key != null) {
                this.keys.add(key);
            }
            return this;
        }

        public Builder fuzzyMode(@Nullable FuzzyMode mode) {
            this.fuzzyMode = mode;
            return this;
        }

        /**
         * @return A {@link FuzzyPartitionList} if a fuzzy mode is set, otherwise a {@link PrecisePartitionList}, or
         *         {@link EmptyPartitionList#INSTANCE} if no key was added.
         */
        public IPartitionList build() {
            if (this.keys.isEmpty()) {
                return EmptyPartitionList.INSTANCE;
            }

            if (this.fuzzyMode != null) {
                final KeyCounter counter = new KeyCounter();
                for (AEKey key : this.keys) {
                    counter.add(key, 1);
                }
                return new FuzzyPartitionList(counter, this.fuzzyMode);
            }
            return new PrecisePartitionList(this.keys);
        }
    }
}
