package com.projecturanus.betterp2p.client

import net.minecraftforge.common.util.ForgeDirection

object ClientCache {
    val positions = mutableListOf<Pair<List<Int?>, ForgeDirection>>()
    var selectedPosition: List<Int?>? = null
    var selectedFacing: ForgeDirection? = null
    var searchText: String= ""
    fun clear() {
        positions.clear()
        selectedPosition = null
        selectedFacing = null
    }
}
