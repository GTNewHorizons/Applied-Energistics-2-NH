package com.projecturanus.betterp2p.network.data;

public final class HashHelper {

    private static final long K2 = 0x9ae16a3b2f90404fL;

    private HashHelper() {}

    public static long hashP2P(int x, int y, int z, int facing, int dim) {
        long ret = (long) facing << 60;
        long lo = (long) x | ((long) y << 32);
        long hi = (long) z | ((long) dim << 32);
        long hash = hashLen16(lo, hi);
        hash ^= hash >>> 59;
        return ret | hash;
    }

    // HashLen16 from CityHash64
    private static long hashLen16(long lo, long hi) {
        long mul = K2 + 32;
        long a = lo + K2;
        long c = Long.rotateRight(hi, 37) * mul + a;
        long d = (Long.rotateRight(a, 25) + hi) * mul;

        long e = (c ^ d) * mul;
        e ^= e >>> 47;
        long f = (d ^ e) * mul;
        f ^= f >>> 47;
        f *= mul;

        return f;
    }
}
