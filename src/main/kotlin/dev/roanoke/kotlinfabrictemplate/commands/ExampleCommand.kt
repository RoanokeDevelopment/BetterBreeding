package dev.roanoke.kotlinfabrictemplate.commands

import com.cobblemon.mod.common.util.permission
import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import dev.roanoke.kotlinfabrictemplate.utils.ExamplePermissions
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text

object GetPokePasteCommand {

    fun register(dispatcher : CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            CommandManager.literal("getPokePaste")
                .permission(ExamplePermissions.GET_POKE_PASTE)
                .then(
                    CommandManager.argument("pokepaste_url", StringArgumentType.greedyString())
                        .executes(::execute)
                ))
    }

    private fun execute(context: CommandContext<ServerCommandSource>) : Int {
        val player: ServerPlayerEntity = context.source.playerOrThrow
        val server: MinecraftServer = context.source.server
        val pokepasteUrl: String = StringArgumentType.getString(context, "pokepaste_url")

        player.sendMessage(Text.literal("Pong: $pokepasteUrl"))

        return Command.SINGLE_SUCCESS
    }

}