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
import net.minecraft.block.entity.HopperBlockEntity
import net.minecraft.inventory.Inventory
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

            player.sendMessage(BetterBreeding.MESSAGES.getDisplayMessage("action.pasture_deactivated"))

            return
        }

        if (BetterBreeding.CONFIG.maxPastures == -1 || BetterBreeding.CONFIG.maxPastures >= playersActivePastures.size) {
            pastureData.activated = true
            playersActivePastures.add(pastureData.uuid)
            activePastures[player.uuid] = playersActivePastures

            player.sendMessage(BetterBreeding.MESSAGES.getDisplayMessage("action.pasture_activated"))

            return
        }

        pastureData.activated = false
        player.sendMessage(BetterBreeding.MESSAGES.getDisplayMessage("error.reached_pasture_limit"))
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

    fun getHopperInventory(world: ServerWorld, pos: BlockPos): Inventory? {

        val blockState = world.getBlockState(pos)
        val blockEntity = world.getBlockEntity(pos)

        if (blockState != null && blockEntity != null) {
            return HopperBlockEntity.getInventoryAt(world, pos)
        }

        return null
    }

    fun onTick(world: ServerWorld, pos: BlockPos, state: BlockState, pasture: PokemonPastureBlockEntity, pastureData: RealPastureData) {

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
            return
        }

        val random = Random.nextDouble()
        if (random < BetterBreeding.CONFIG.eggCheckChance) {
            pastureData.ticksTilCheck = BetterBreeding.CONFIG.eggCheckTicks
            return
        }

        val pokemon: List<Pokemon> = pasture.tetheredPokemon.getPokemon().filterNotNull()

        BreedingUtils.applyMirrorHerb(pokemon)

        val eggInfo: EggInfo? = BreedingUtils.chooseEgg(pokemon)
        if (eggInfo == null) {
            pastureData.ticksTilCheck = BetterBreeding.CONFIG.eggCheckTicks
            return
        }

        pastureData.eggInfo = eggInfo

        Rib.server?.playerManager?.playerList?.find {
                    it.uuid == pasture.ownerId
                }?.sendMessage(
                    BetterBreeding.MESSAGES.getDisplayMessage("message.found_egg_in_pasture")
                )

        if (BetterBreeding.CONFIG.allowHoppers) {
            getHopperInventory(world, pos.down())?.let { inventory ->
                for (i in 0 until inventory.size()) {
                    if (inventory.getStack(i).isEmpty) {
                        inventory.setStack(i, EggItem.getEggItem(pastureData.eggInfo!!))
                        pastureData.eggInfo = null
                        pastureData.ticksTilCheck = BetterBreeding.CONFIG.eggCheckTicks
                        break
                    }
                }
            }
        }

    }

    fun onBreak(world: ServerWorld, pos: BlockPos, state: BlockState, player: ServerPlayerEntity, pasture: PokemonPastureBlockEntity, pastureData: RealPastureData) {

        if (!BetterBreeding.CONFIG.useRealPastures()) {
            return
        }

        if (pastureData.eggInfo != null) {
            player.sendMessage(BetterBreeding.MESSAGES.getDisplayMessage("action.claimed_broken_pasture_egg"))
            player.giveOrDropItemStack(EggItem.getEggItem(pastureData.eggInfo!!), true)
        }

        activePastures.forEach {
            it.value.remove(pastureData.uuid)
        }

    }

    fun onUse(world: ServerWorld, player: ServerPlayerEntity, hand: Hand, hit: BlockHitResult, pastureData: RealPastureData): Boolean { // cancel?

        val usedItem = player.getStackInHand(hand)

        if (BetterBreeding.ITEMS.getItemBuilder("pastureActivationItem").matches(usedItem)) {
            toggleActivePasture(player, pastureData)
            return true
        }

        if (pastureData.eggInfo != null) {
            player.sendMessage(BetterBreeding.MESSAGES.getDisplayMessage("action.claimed_egg_from_hutch"))
            player.giveOrDropItemStack(EggItem.getEggItem(pastureData.eggInfo!!), true)
            pastureData.eggInfo = null
            pastureData.ticksTilCheck = BetterBreeding.CONFIG.eggCheckTicks
            return true
        }

        return false
    }

}