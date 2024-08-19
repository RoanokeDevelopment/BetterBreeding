package dev.roanoke.betterbreeding.pastures.real

import com.cobblemon.mod.common.block.entity.PokemonPastureBlockEntity
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.util.giveOrDropItemStack
import dev.roanoke.betterbreeding.BetterBreeding
import dev.roanoke.betterbreeding.breeding.EggInfo
import dev.roanoke.betterbreeding.breeding.BreedingUtils
import dev.roanoke.betterbreeding.breeding.BreedingUtils.getPokemon
import dev.roanoke.betterbreeding.items.EggItem
import dev.roanoke.rib.Rib
import net.minecraft.block.BlockState
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos
import kotlin.random.Random

object RealPastureManager {

    val pastures: MutableList<RealPastureData> = mutableListOf()

    /*
    fun isPastureActive(pasture: PokemonPastureBlockEntity): Boolean {
        return pastures.count {
            it.owner == pasture.ownerId
        } < 2
    }

     */

    fun getPastureData(pasture: PokemonPastureBlockEntity): RealPastureData? {
        var pastureData = pastures.find { it.code == pasture.hashCode() }
        if (pastureData == null) {
            pastureData = RealPastureData.fromPastureEntity(pasture)
            pastureData?.let {
                pastures.add(pastureData)
                Rib.LOGGER.info("Created and stored NEW pasture data for ${pasture.pos}")
            }
        }
        return pastureData
    }

    fun onTick(world: ServerWorld, pos: BlockPos, state: BlockState, pasture: PokemonPastureBlockEntity) {

        if (!BetterBreeding.CONFIG.useRealPastures()) {
            return
        }

        getPastureData(pasture)?.let { pastureData ->

            if (pastureData.eggInfo != null) {
                return
            }

            pastureData.ticksTilCheck--
            if (pastureData.ticksTilCheck > 0) {
                Rib.LOGGER.info("Ticks til check for ${pos}: ${pastureData.ticksTilCheck}")
                return
            }

            // no egg, is active, and is time to check for egg

            if (Random.nextDouble() < BetterBreeding.CONFIG.eggCheckChance) {
                Rib.LOGGER.info("Didn't create Egg in Pasture, failed random check.")
                pastureData.ticksTilCheck = BetterBreeding.CONFIG.eggCheckTicks
                return
            }

            val pokemon: List<Pokemon> = pasture.tetheredPokemon.getPokemon().filterNotNull()

            BreedingUtils.applyMirrorHerb(pokemon)

            val eggInfo: EggInfo? = BreedingUtils.chooseEgg(pokemon)
            if (eggInfo == null) {
                Rib.LOGGER.info("Checking Pasture ${pos}: They Can't even Produce an EGG!")
                pastureData.ticksTilCheck = BetterBreeding.CONFIG.eggCheckTicks
                return
            }

            pastureData.eggInfo = eggInfo

            Rib.server?.playerManager?.playerList?.find {
                        it.uuid == pastureData.owner
                    }?.sendMessage(
                Text.literal("Hey, we found an egg in your REAL pasture!"))

        }
    }

    fun onBreak(world: ServerWorld, pos: BlockPos, state: BlockState, player: ServerPlayerEntity, pasture: PokemonPastureBlockEntity) {
        Rib.LOGGER.info("On Break from RealPastuerManager success called")

        if (!BetterBreeding.CONFIG.useRealPastures()) {
            return
        }

        getPastureData(pasture)?.let { pastureData ->
            Rib.LOGGER.info("Removing pasture data /checking for and giving egg for that OnBreak call")
            pastures.remove(pastureData)
            if (pastureData.eggInfo != null) {
                Rib.LOGGER.info("They should be getting an egg, cuz there was one in pasture data!")
                player.sendMessage(Text.literal("Hey, don't forget your egg!"))
                player.giveOrDropItemStack(EggItem.getEggItem(pastureData.eggInfo!!), true)
            }
        }

    }

}