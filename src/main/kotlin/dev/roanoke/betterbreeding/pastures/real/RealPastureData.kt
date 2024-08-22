package dev.roanoke.betterbreeding.pastures.real

import com.cobblemon.mod.common.block.entity.PokemonPastureBlockEntity
import dev.roanoke.betterbreeding.BetterBreeding
import dev.roanoke.betterbreeding.breeding.EggInfo
import dev.roanoke.rib.Rib
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import java.util.UUID

data class RealPastureData(
    var uuid: UUID,
    var activated: Boolean,
    var ticksTilCheck: Int,
    var eggInfo: EggInfo?
) {
    companion object {
        fun fromPastureEntity(pasture: PokemonPastureBlockEntity): RealPastureData {
            return RealPastureData(
                uuid = UUID.randomUUID(),
                activated = false,
                ticksTilCheck = BetterBreeding.CONFIG.eggCheckTicks,
                eggInfo = null
            )
        }

        fun fromNbtOrPasture(nbt: NbtCompound, pasture: PokemonPastureBlockEntity): RealPastureData {
            Rib.LOGGER.info("READING RealPastureData from NBT")
            val ticksTilCheck: Int
            if (nbt.contains("ticksTilCheck")) {
                ticksTilCheck = nbt.getInt("ticksTilCheck")
            } else {
                Rib.LOGGER.info("Pasture had NO PASTURE NBT (ticksTilCheck) so LOADING FROM ENTITY")
                return fromPastureEntity(pasture)
            }

            val uuid: UUID
            if (nbt.contains("pastureUUID")) {
                uuid = nbt.getUuid("pastureUUID")
            } else {
                Rib.LOGGER.info("Pasture had no PASTURE UUID")
                return fromPastureEntity(pasture)
            }

            val activated: Boolean
            if (nbt.contains("activated")) {
                activated = nbt.getBoolean("activated")
            } else {
                Rib.LOGGER.info("Pasture had no PASTURE ACTIVATED")
                return fromPastureEntity(pasture)
            }

            var eggInfo: EggInfo? = null
            if (nbt.contains("eggInfo")) {
                eggInfo = EggInfo.fromNbt(nbt.getCompound("eggInfo"))
            }

            return RealPastureData(uuid, activated, ticksTilCheck, eggInfo)
        }

    }

    fun toNbt(nbt: NbtCompound) {
        Rib.LOGGER.info("SAVING REAL PASTURE DATA NBT - YIPPIE!")
        nbt.putInt("ticksTilCheck", ticksTilCheck)
        if (eggInfo != null) {
            val eggInfoNbt = NbtCompound()
            eggInfo!!.toNbt(eggInfoNbt)
            nbt.put("eggInfo", eggInfoNbt)
        }
        nbt.putBoolean("activated", activated)
        nbt.putUuid("pastureUUID", uuid)
    }

}