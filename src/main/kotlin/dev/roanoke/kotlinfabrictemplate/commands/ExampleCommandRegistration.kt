package dev.roanoke.kotlinfabrictemplate.commands

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource

object ExampleCommandRegistration {
    fun register(dispatcher: CommandDispatcher<ServerCommandSource>, registry: CommandRegistryAccess, selection: CommandManager.RegistrationEnvironment) {
        GetPokePasteCommand.register(dispatcher)
    }
}