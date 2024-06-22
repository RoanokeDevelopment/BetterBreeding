package dev.roanoke.betterbreeding.commands

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource

object BetterBreedingCommandRegistration {
    fun register(dispatcher: CommandDispatcher<ServerCommandSource>, registry: CommandRegistryAccess, selection: CommandManager.RegistrationEnvironment) {
        BetterBreedingCommands.register(dispatcher)
    }
}