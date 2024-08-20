package dev.roanoke.betterbreeding.commands

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.util.giveOrDropItemStack
import com.cobblemon.mod.common.util.permission
import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.context.CommandContext
import dev.roanoke.betterbreeding.BetterBreeding
import dev.roanoke.betterbreeding.BetterBreeding.Companion.MAIN_DIR
import dev.roanoke.betterbreeding.breeding.EggInfo
import dev.roanoke.betterbreeding.breeding.BreedingUtils
import dev.roanoke.betterbreeding.items.EggItem
import dev.roanoke.betterbreeding.utils.BetterBreedingPermissions
import dev.roanoke.betterbreeding.utils.Config
import dev.roanoke.rib.utils.Messages
import net.impactdev.impactor.api.economy.EconomyService
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import kotlin.system.measureTimeMillis

object BetterBreedingCommands {

    fun register(dispatcher : CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            CommandManager.literal("forceBreed")
                .permission(BetterBreedingPermissions.FORCE_BREED)
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
                .permission(BetterBreedingPermissions.FORCE_HATCH)
                .executes(this::forceHatch)
        )
        dispatcher.register(
            CommandManager.literal("eggInfo")
                .permission(BetterBreedingPermissions.EGG_INFO)
                .executes(this::eggInfo)
        )
        dispatcher.register(
            CommandManager.literal("openDayCare")
                .permission(BetterBreedingPermissions.OPEN_DAY_CARE)
                .then(
                    CommandManager.argument("player", EntityArgumentType.player())
                        .executes(this::openDayCare)
                )
        )
        dispatcher.register(
            CommandManager.literal("daycare")
                .permission(BetterBreedingPermissions.DAY_CARE)
                .executes(this::dayCare)
        )
        dispatcher.register(
            CommandManager.literal("betterbreeding")
                .permission(BetterBreedingPermissions.RELOAD)
                .then(
                    CommandManager.literal("reload")
                        .executes(::reload)
                )
        )
        dispatcher.register(
            CommandManager.literal("neuter")
                .permission(BetterBreedingPermissions.NEUTER)
                .then(
                    CommandManager.argument("slot", IntegerArgumentType.integer(1, 6))
                        .executes(::neuter)
                )
        )
    }

    private fun neuter(ctx: CommandContext<ServerCommandSource>) : Int {
        val player = ctx.source.player ?: return 1
        val slot = IntegerArgumentType.getInteger(ctx, "slot") - 1

        val party = Cobblemon.storage.getParty(player)

        val pokemon = party.get(slot)

        if (pokemon == null) {
            player.sendMessage(BetterBreeding.MESSAGES.getDisplayMessage("error.no_pokemon_in_slot"))
            return 1
        }

        if (pokemon.persistentData.contains("neuterer")) {
            player.sendMessage(BetterBreeding.MESSAGES.getDisplayMessage("error.pokemon_already_neutered"))
            return 1
        }

        if (BetterBreeding.CONFIG.neuterCost >= 0) {
            val economy = EconomyService.instance()
            val currency = economy.currencies().primary()
            val account = economy.account(currency, player.uuid).get()

            if (account.balanceAsync().get() >= BetterBreeding.CONFIG.neuterCost.toBigDecimal()) {
                account.withdrawAsync(BetterBreeding.CONFIG.neuterCost.toBigDecimal()).get().successful()
            } else {
                player.sendMessage(BetterBreeding.MESSAGES.getDisplayMessage("error.cant_afford_neuter_cost"))
                return 1
            }
        }

        pokemon.persistentData.putBoolean("breedable", false)
        pokemon.persistentData.putUuid("neuterer", player.uuid)

        player.sendMessage(BetterBreeding.MESSAGES.getDisplayMessage("action.pokemon_neutered"))

        return 1

    }

    private fun reload(context: CommandContext<ServerCommandSource>) : Int {
        val timeTaken = measureTimeMillis {
            BetterBreeding.MESSAGES = Messages(MAIN_DIR.resolve("messages.json"), "/betterbreeding/messages.json")
            context.source.sendMessage(Text.literal("Reloaded BetterBreeding Messages"))
            BetterBreeding.GUIs.reload()
            context.source.sendMessage(Text.literal("Reloaded BetterBreeding GUIs"))
            BetterBreeding.ITEMS.reload()
            context.source.sendMessage(Text.literal("Reloaded BetterBreeding Egg Item Config"))
            BetterBreeding.CONFIG = Config.load(MAIN_DIR.resolve("config.json").toFile())
            context.source.sendMessage(Text.literal("Reloaded BetterBreeding Breeding Config"))
        }
        context.source.sendMessage(Text.literal("Reloaded BetterBreeding in ${timeTaken}ms"))
        return 1
    }

    private fun dayCare(context: CommandContext<ServerCommandSource>) : Int {

        if (!BetterBreeding.CONFIG.useVirtualPastures()) {
            return 1
        }

        context.source.player?.let {
            BetterBreeding.PASTURES.openPasture(it)
        }
        return 1
    }

    private fun openDayCare(context: CommandContext<ServerCommandSource>) : Int {

        if (!BetterBreeding.CONFIG.useVirtualPastures()) {
            return 1
        }

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

                    item.removeSubNbt("species")
                    item.decrement(1)

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

        val eggInfo = BreedingUtils.chooseEgg(monChoices)

        if (eggInfo == null) {
            player.sendMessage(Text.literal("Egg Info was null so they cant breed ig"))
            return Command.SINGLE_SUCCESS
        }

        val eggItem = EggItem.getEggItem(eggInfo)
        player.giveOrDropItemStack(eggItem, true)

        return Command.SINGLE_SUCCESS
    }

}