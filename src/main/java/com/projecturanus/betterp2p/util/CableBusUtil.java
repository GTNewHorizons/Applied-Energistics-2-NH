package com.projecturanus.betterp2p.util;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;
import net.minecraft.world.IBlockAccess;

import org.jspecify.annotations.Nullable;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.parts.SelectedPart;

public final class CableBusUtil {

    private CableBusUtil() {}

    public static @Nullable IPart getPart(IBlockAccess w, int posX, int posY, int posZ, float hitX, float hitY,
            float hitZ) {
        TileEntity te = w.getTileEntity(posX, posY, posZ);
        if (!(te instanceof IPartHost host)) {
            return null;
        }
        SelectedPart p = host.selectPart(Vec3.createVectorHelper(hitX, hitY, hitZ));
        return p != null ? p.part : null;
    }
}
