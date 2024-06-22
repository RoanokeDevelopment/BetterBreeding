package dev.roanoke.kotlinfabrictemplate

import net.fabricmc.api.ModInitializer
import dev.roanoke.kotlinfabrictemplate.commands.ExampleCommandRegistration
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback


class KotlinFabricTemplate : ModInitializer {

    companion object {

        val MODID = "kotlinfabrictemplate"

    }

    override fun onInitialize() {
        CommandRegistrationCallback.EVENT.register(ExampleCommandRegistration::register)
    }

}
