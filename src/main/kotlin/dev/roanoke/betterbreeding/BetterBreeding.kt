package dev.roanoke.betterbreeding

import net.fabricmc.api.ModInitializer
import dev.roanoke.betterbreeding.commands.BetterBreedingCommandRegistration
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback


class BetterBreeding : ModInitializer {

    companion object {

        val MODID = "betterbreeding"

    }

    override fun onInitialize() {
        CommandRegistrationCallback.EVENT.register(BetterBreedingCommandRegistration::register)
    }

}
