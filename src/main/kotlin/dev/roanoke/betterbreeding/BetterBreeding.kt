package dev.roanoke.betterbreeding

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.moves.BenchedMove
import dev.roanoke.betterbreeding.breeding.EggInfo
import net.fabricmc.api.ModInitializer
import dev.roanoke.betterbreeding.commands.BetterBreedingCommandRegistration
import dev.roanoke.betterbreeding.items.EggItem
import dev.roanoke.betterbreeding.utils.Config
import dev.roanoke.rib.callbacks.RibInitCallback
import dev.roanoke.rib.gui.configurable.CGuiManager
import dev.roanoke.rib.utils.FileUtils
import dev.roanoke.rib.utils.ItemManager
import dev.roanoke.rib.utils.Messages
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.text.Text


class BetterBreeding : ModInitializer {

    companion object {

        val MODID = "betterbreeding"

        val MAIN_DIR = FabricLoader.getInstance().configDir.resolve("BetterBreeding")

        val PASTURES_DIR = MAIN_DIR.resolve("Pastures")

        val GUI_DIR = MAIN_DIR.resolve("GUI")
        val MENU_DIR = GUI_DIR.resolve("Menus")
        val ITEMS_PATH = MAIN_DIR.resolve("items.json")

        val MESSAGES: Messages = Messages(MAIN_DIR.resolve("messages.json"), "/betterbreeding/messages.json")
        val GUIs: CGuiManager = CGuiManager(MENU_DIR, GUI_DIR.resolve("item_definitions.json"))
        val ITEMS: ItemManager = ItemManager(ITEMS_PATH, "/betterbreeding/items.json")
        val PASTURES: PastureManager = PastureManager()

        val CONFIG: Config = Config.load(MAIN_DIR.resolve("config.json").toFile())

    }

    override fun onInitialize() {
        CommandRegistrationCallback.EVENT.register(BetterBreedingCommandRegistration::register)

        FileUtils.copyResourceToFile("/betterbreeding/gui/menus/virtual_pasture.json", MENU_DIR.resolve("virtual_pasture.json"))
        FileUtils.copyResourceToFile("/betterbreeding/gui/menus/virtual_pasture_pokeswapper.json", MENU_DIR.resolve("virtual_pasture_pokeswapper.json"))
        FileUtils.copyResourceToFile("/betterbreeding/gui/item_definitions.json", GUI_DIR.resolve("item_definitions.json"))

        RibInitCallback.EVENT.register {
            ITEMS.setup()
            GUIs.setup()
            PASTURES.setup()
        }

        ServerTickEvents.START_SERVER_TICK.register { server ->
            server.playerManager.playerList.forEach { player ->

                player.inventory.main.iterator().forEach {
                    EggItem.tryTickEgg(it, player)
                }

                player.inventory.offHand.iterator().forEach {
                    EggItem.tryTickEgg(it, player)
                }

            }
        }

    }

}
