package dev.roanoke.betterbreeding

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.moves.BenchedMove
import dev.roanoke.betterbreeding.breeding.EggInfo
import net.fabricmc.api.ModInitializer
import dev.roanoke.betterbreeding.commands.BetterBreedingCommandRegistration
import dev.roanoke.betterbreeding.items.EggItem
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.text.Text


class BetterBreeding : ModInitializer {

    companion object {

        val MODID = "betterbreeding"

    }

    override fun onInitialize() {
        CommandRegistrationCallback.EVENT.register(BetterBreedingCommandRegistration::register)

        ServerTickEvents.START_SERVER_TICK.register { server ->
            server.playerManager.playerList.forEach { player ->
                player.inventory.combinedInventory.forEach { df -> df.forEach { stack ->

                    if (EggItem.isEgg(stack)) {
                        val timer = stack.nbt?.getInt("timer") ?: 100
                        stack.orCreateNbt.putInt("timer", timer - 1)

                        val party = Cobblemon.storage.getParty(player.uuid)
                        for (i in 0..5) {
                            val pokemon = party.get(i)
                            // Looking for an ability reducing hatching time in party
                            val incubator = pokemon?.let {
                                val ability = it.ability.template.name
                                (ability == "magmaarmor" || ability == "flamebody" || ability == "steamengine")
                            } ?: false
                            if (incubator) {
                                stack.getOrCreateNbt().putInt("timer", timer - 2)
                                break
                            }
                        }

                        if (timer <= 0) {
                            val info = stack.nbt?.let { EggInfo.fromNbt(it) }
                            if (info != null) {
                                stack.removeSubNbt("species")
                                stack.decrement(1)

                                val pokemon = info.getPokemon()

                                for (move in info.eggMoves ?: setOf()) {
                                    // Always adding the move to benched moves to easily add it in its allAccessibleMoves so
                                    // the pokemon will be able to transfer its eggmoves to its children anytime
                                    pokemon.benchedMoves.add(BenchedMove(move, 0))
                                    if (pokemon.moveSet.hasSpace()) {
                                        pokemon.moveSet.add(move.create())
                                    }
                                }

                                party.add(pokemon)

                                player.sendMessage(Text.literal("Your Egg hatched!"))
                            }
                        }

                    }

                }}
            }
        }

    }

}
