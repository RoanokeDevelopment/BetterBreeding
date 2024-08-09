package dev.roanoke.betterbreeding.commands

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.util.giveOrDropItemStack
import com.cobblemon.mod.common.util.permission
import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import dev.roanoke.betterbreeding.BetterBreeding
import dev.roanoke.betterbreeding.breeding.EggInfo
import dev.roanoke.betterbreeding.breeding.PastureUtils
import dev.roanoke.betterbreeding.items.EggItem
import dev.roanoke.betterbreeding.utils.ExamplePermissions
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text

object BetterBreedingCommands {

    fun register(dispatcher : CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            CommandManager.literal("forceBreed")
                .permission(ExamplePermissions.FORCE_BREED)
                .then(
                    CommandManager.argument("slot1", IntegerArgumentType.integer(1, 6))
                        .then(
                            CommandManager.argument("slot2", IntegerArgumentType.integer(1, 6))
                                .executes(this::forceBreed)
                        )
                )
        );
        dispatcher.register(
            CommandManager.literal("forceHatch")
                .permission(ExamplePermissions.FORCE_HATCH)
                .executes(this::forceHatch)
        )
        dispatcher.register(
            CommandManager.literal("eggInfo")
                .permission(ExamplePermissions.EGG_INFO)
                .executes(this::eggInfo)
        )
        dispatcher.register(
            CommandManager.literal("openDayCare")
                .permission(ExamplePermissions.OPEN_DAY_CARE)
                .then(
                    CommandManager.argument("player", EntityArgumentType.player())
                        .executes(this::openDayCare)
                )
        )
    }

    private fun openDayCare(context: CommandContext<ServerCommandSource>) : Int {
        try {
            val target = EntityArgumentType.getPlayer(context, "player") ?: return 1
            BetterBreeding.PASTURES.openPasture(target);
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 1
    }

    private fun forceHatch(context: CommandContext<ServerCommandSource>) : Int {
        val player: ServerPlayerEntity = context.source.playerOrThrow
        val party = Cobblemon.storage.getParty(player.uuid)

        player.sendMessage(Text.literal("Looking for eggs you're carrying..."))

        player.handItems.forEach { item ->
            if (EggItem.isEgg(item)) {
                val eggInfo: EggInfo? = EggItem.getEggInfo(item)
                if (eggInfo != null) {
                    party.add(
                        eggInfo.getPokemon()
                    )
                    player.sendMessage(Text.literal("Your egg hatched!"))
                }
            }
        }

        return Command.SINGLE_SUCCESS
    }

    private fun eggInfo(context: CommandContext<ServerCommandSource>) : Int {
        val player: ServerPlayerEntity = context.source.playerOrThrow

        player.handItems.forEach { item ->
            if (EggItem.isEgg(item)) {
                val eggInfo: EggInfo? = EggItem.getEggInfo(item)
                if (eggInfo != null) {
                    val timer = item.nbt?.getInt("timer") ?: 100
                    player.sendMessage(Text.literal("Egg Timer: $timer"))
                    player.sendMessage(Text.literal("Egg Species: ${eggInfo.species?.name}"))
                }
            }
        }

        return Command.SINGLE_SUCCESS
    }

    private fun forceBreed(context: CommandContext<ServerCommandSource>) : Int {
        val player: ServerPlayerEntity = context.source.playerOrThrow

        val slot1: Int = IntegerArgumentType.getInteger(context, "slot1") - 1
        val slot2: Int = IntegerArgumentType.getInteger(context, "slot2") - 1

        val party = Cobblemon.storage.getParty(player.uuid)

        val monChoices: List<Pokemon?> = listOf(
            party.get(slot1),
            party.get(slot2)
        )

        val eggInfo = PastureUtils.chooseEgg(monChoices)

        if (eggInfo == null) {
            player.sendMessage(Text.literal("Egg Info was null so they cant breed ig"))
            return Command.SINGLE_SUCCESS
        }

        val eggItem = EggItem.getEggItem(eggInfo)
        player.giveOrDropItemStack(eggItem, true)

        return Command.SINGLE_SUCCESS
    }

}