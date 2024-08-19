package dev.roanoke.betterbreeding.pastures.real

import com.cobblemon.mod.common.block.entity.PokemonPastureBlockEntity
import dev.roanoke.betterbreeding.BetterBreeding
import dev.roanoke.betterbreeding.breeding.EggInfo
import java.util.UUID

data class RealPastureData(
    val code: Int,
    val owner: UUID,
    var ticksTilCheck: Int,
    var eggInfo: EggInfo?
) {
    companion object {
        fun fromPastureEntity(pasture: PokemonPastureBlockEntity): RealPastureData? {
            return RealPastureData(
                code = pasture.hashCode(),
                owner = pasture.ownerId ?: return null,
                ticksTilCheck = BetterBreeding.CONFIG.eggCheckTicks,
                eggInfo = null
            )
        }
    }
}