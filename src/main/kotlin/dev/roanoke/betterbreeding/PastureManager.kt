package dev.roanoke.betterbreeding

import dev.roanoke.betterbreeding.breeding.VirtualPasture
import net.minecraft.server.network.ServerPlayerEntity

class PastureManager {

    val pastures: MutableList<VirtualPasture> = mutableListOf()

    fun setup() {

    }

    fun savePasture(pasture: VirtualPasture) {
        println("pretending to save")
    }

    fun openPasture(player: ServerPlayerEntity) {
        var pasture = pastures.find {
            it.player == player.uuid
        }
        if (pasture == null) {
            pasture = VirtualPasture(player.uuid, mutableListOf(), null)
            pastures.add(pasture)
        }

        pasture.openPastureGui(player)
    }

}