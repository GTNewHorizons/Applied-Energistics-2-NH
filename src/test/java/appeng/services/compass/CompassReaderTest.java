/*
 * This file is part of Applied Energistics 2. Copyright (c) 2013 - 2015, AlgorithmX2, All rights reserved. Applied
 * Energistics 2 is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version. Applied Energistics 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details. You should have received a copy of the GNU Lesser General Public License along with
 * Applied Energistics 2. If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.services.compass;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CompassReaderTest {

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void targetsStayIndependentAcrossReopen() throws IOException {
        final File directory = this.folder.newFolder();
        final CompassReader stones = new CompassReader(0, directory, false);
        final CompassReader chests = new CompassReader(0, directory, true);
        try {
            stones.setHasBeacon(2, 3, 0, true);
            chests.setHasBeacon(2, 3, 0, true);
            chests.setHasBeacon(2, 3, 0, false);
            chests.setHasBeacon(3, 3, 0, true);
        } finally {
            stones.close();
            chests.close();
        }

        try {
            Assert.assertTrue(stones.hasBeacon(2, 3));
            Assert.assertFalse(chests.hasBeacon(2, 3));
            Assert.assertTrue(chests.hasBeacon(3, 3));
            Assert.assertFalse(stones.hasBeacon(3, 3));
        } finally {
            stones.close();
            chests.close();
        }
    }

    @Test
    public void negativeZRegionsKeepTheirOwnTargets() throws IOException {
        final CompassReader chests = new CompassReader(0, this.folder.newFolder(), true);
        try {
            chests.setHasBeacon(0, -1, 0, true);

            Assert.assertFalse(chests.hasBeacon(1024, -1));
            Assert.assertTrue(chests.hasBeacon(0, -1));
        } finally {
            chests.close();
        }
    }
}
