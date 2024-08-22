package dev.roanoke.betterbreeding.pastures.real

import com.cobblemon.mod.common.api.events.CobblemonEvents
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
import net.minecraft.item.Items
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import java.util.UUID
import kotlin.random.Random

object RealPastureManager {

    var activePastures: MutableMap<UUID, MutableSet<UUID>> = mutableMapOf()

    fun getActivePastures(player: ServerPlayerEntity): MutableSet<UUID> {
        return activePastures.getOrDefault(player.uuid, mutableSetOf())
    }

    fun toggleActivePasture(player: ServerPlayerEntity, pastureData: RealPastureData) {
        val playersActivePastures = activePastures.getOrDefault(player.uuid, mutableSetOf())

        if (playersActivePastures.contains(pastureData.uuid)) {
            playersActivePastures.remove(pastureData.uuid)
            pastureData.activated = false

            player.sendMessage(Text.literal("Pasture deactivated"))

            return
        }

        if (BetterBreeding.CONFIG.maxPastures == -1 || BetterBreeding.CONFIG.maxPastures >= playersActivePastures.size) {
            pastureData.activated = true
            playersActivePastures.add(pastureData.uuid)
            activePastures[player.uuid] = playersActivePastures

            player.sendMessage(Text.literal("Pasture activated!"))
            return
        }

        pastureData.activated = false
        player.sendMessage(Text.literal("You've reached your Pasture Limit!"))
        return
    }

    fun checkActive(playerUUID: UUID, pastureData: RealPastureData): Boolean {
        val playersActivePastures = activePastures.getOrDefault(playerUUID, mutableSetOf())

        if (!pastureData.activated) return false

        if (playersActivePastures.contains(pastureData.uuid)) return true

        if (playersActivePastures.size >= BetterBreeding.CONFIG.maxPastures) {
            pastureData.activated = false
            return false
        } else {
            playersActivePastures.add(pastureData.uuid)
            return true
        }

    }

    fun onTick(world: ServerWorld, pos: BlockPos, state: BlockState, pasture: PokemonPastureBlockEntity, pastureData: RealPastureData) {

        //Rib.LOGGER.info("Got onTick yippie!")

        if (!BetterBreeding.CONFIG.useRealPastures()) {
            return
        }

        if (pastureData.eggInfo != null) {
            return
        }

        if (pasture.tetheredPokemon.isEmpty()) {
            return
        }

        if (!checkActive(pasture.ownerId!!, pastureData)) {
            return
        }

        pastureData.ticksTilCheck--
        if (pastureData.ticksTilCheck > 0) {
            Rib.LOGGER.info("Ticks Til Check: ${pastureData.ticksTilCheck}")
            return
        }

        // no egg, is active, and is time to check for egg

        val random = Random.nextDouble()
        if (random < BetterBreeding.CONFIG.eggCheckChance) {
            Rib.LOGGER.info("Didn't create Egg in Pasture, failed random check. Random var: ${random}, Egg Check Chance: ${BetterBreeding.CONFIG.eggCheckChance}")
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
                    it.uuid == pasture.ownerId
                }?.sendMessage(
            Text.literal("Hey, we found an egg in your REAL pasture!"))

    }

    fun onBreak(world: ServerWorld, pos: BlockPos, state: BlockState, player: ServerPlayerEntity, pasture: PokemonPastureBlockEntity, pastureData: RealPastureData) {
        Rib.LOGGER.info("On Break from RealPastuerManager success called")

        if (!BetterBreeding.CONFIG.useRealPastures()) {
            return
        }

        Rib.LOGGER.info("Removing pasture data /checking for and giving egg for that OnBreak call")
        if (pastureData.eggInfo != null) {
            Rib.LOGGER.info("They should be getting an egg, cuz there was one in pasture data!")
            player.sendMessage(Text.literal("Hey, don't forget your egg!"))
            player.giveOrDropItemStack(EggItem.getEggItem(pastureData.eggInfo!!), true)
        }

        activePastures.forEach {
            it.value.remove(pastureData.uuid)
        }

    }

    fun onUse(world: ServerWorld, player: ServerPlayerEntity, hand: Hand, hit: BlockHitResult, pastureData: RealPastureData): Boolean { // cancel?

        val usedItem = player.getStackInHand(hand)
        Rib.LOGGER.info("Used Pasture with Item ${usedItem.item.name}")

        if (usedItem.isOf(Items.WOODEN_HOE)) {
            toggleActivePasture(player, pastureData)
            return true
        }

        if (pastureData.eggInfo != null) {
            player.sendMessage(Text.literal("Here's the Egg we found!"))
            player.giveOrDropItemStack(EggItem.getEggItem(pastureData.eggInfo!!), true)
            pastureData.eggInfo = null
            pastureData.ticksTilCheck = BetterBreeding.CONFIG.eggCheckTicks
            return true
        }

        return false
    }

}