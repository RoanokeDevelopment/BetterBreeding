package dev.roanoke.betterbreeding

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import dev.roanoke.betterbreeding.breeding.EggInfo
import dev.roanoke.betterbreeding.breeding.PastureUtils
import dev.roanoke.betterbreeding.breeding.VirtualPasture
import dev.roanoke.rib.Rib
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import java.io.File
import java.nio.file.Files
import kotlin.random.Random

class PastureManager {

    val pastures: MutableList<VirtualPasture> = mutableListOf()

    init {
        ServerTickEvents.START_SERVER_TICK.register {
            pastures.forEach { pasture ->
                pasture.ticksTilCheck--
                if (pasture.ticksTilCheck <= 0) {

                    pasture.ticksTilCheck = BetterBreeding.CONFIG.eggCheckTicks
                    if (pasture.egg != null) {
                        return@forEach
                    }

                    if (Random.nextDouble() < BetterBreeding.CONFIG.eggCheckChance) {
                        Rib.LOGGER.info("Didn't create Egg in Pasture, failed random check.")
                        return@forEach
                    }

                    PastureUtils.applyMirrorHerb(pasture.pokemon)

                    var eggInfo: EggInfo? = PastureUtils.chooseEgg(pasture.pokemon)
                    if (eggInfo == null) {
                        Rib.LOGGER.info("Checking Pasture: They Can't even Produce an EGG!")
                        return@forEach
                    }

                    pasture.egg = eggInfo
                    this.savePasture(pasture)

                    Rib.server?.playerManager?.playerList?.find { it.uuid == pasture.player }
                        ?.sendMessage(Text.literal("Hey! We found an Egg for you in the Day Care!"))

                }
            }
        }
    }

    fun setup() {
        Files.createDirectories(BetterBreeding.PASTURES_DIR)

        val files = BetterBreeding.PASTURES_DIR.toFile().listFiles { _, name -> name.endsWith(".json") } ?: return
        val gson = Gson()
        files.forEach { file ->
            try {
                val json = gson.fromJson(file.readText(), JsonObject::class.java)
                val pasture = VirtualPasture.fromJson(json)
                pastures.add(pasture)
            } catch (e: Exception) {
                Rib.LOGGER.error("Failed to load pasture from file: ${file.name}", e)
            }
        }
    }

    fun savePasture(pasture: VirtualPasture) {

        val directory: File = BetterBreeding.PASTURES_DIR.toFile()
        if (!directory.exists()) {
            directory.mkdirs()
        }

        val saveFile: File = BetterBreeding.PASTURES_DIR.resolve("${pasture.player}.json").toFile()
        val prettyJsonString = GsonBuilder().setPrettyPrinting().create().toJson(pasture.toJson())
        saveFile.writeText(prettyJsonString)
    }

    fun openPasture(player: ServerPlayerEntity) {
        var pasture = pastures.find {
            it.player == player.uuid
        }
        if (pasture == null) {
            pasture = VirtualPasture(player.uuid, mutableListOf(), null)
            pastures.add(pasture)
        }

        pasture.openPastureGui(player)
    }

}