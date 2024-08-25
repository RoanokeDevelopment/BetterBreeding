package dev.roanoke.betterbreeding.utils

import com.cobblemon.mod.common.api.pokemon.PokemonProperties
import com.cobblemon.mod.common.pokemon.Pokemon
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileReader
import java.io.FileWriter

data class Config(
    var eggCheckTicks: Int = 12000,
    var eggCheckChance: Double = 0.5,
    val eggHatchMultiplier: Double = 1.0,
    val enabledHiddenAbilities: Boolean = true,
    val shinyMethod: String = "disabled",
    val shinyMultiplier: Float = 4.0f,
    val pastureMethod: String = "both", // could be "virtual", "real", or "both"
    val neuterCost: Int = 0,
    val maxPastures: Int = 3,
    val allowHoppers: Boolean = false,
    val propertiesBlacklist: List<String> = listOf()
) {

    companion object {
        private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

        fun load(file: File): Config {
            file.parentFile.mkdirs()

            return if (!file.exists()) {
                val defaultConfig = Config()
                save(defaultConfig, file)
                defaultConfig
            } else {
                val config = FileReader(file).use { reader ->
                    gson.fromJson(reader, Config::class.java)
                }
                save(config, file)
                config
            }
        }

        fun save(config: Config, file: File) {
            FileWriter(file).use { writer ->
                gson.toJson(config, writer)
            }
        }
    }

    fun useVirtualPastures(): Boolean {
        return pastureMethod == "virtual" || pastureMethod == "both"
    }

    fun useRealPastures(): Boolean {
        return pastureMethod == "real" || pastureMethod == "both"
    }

    fun passesBlacklist(pokemon: Pokemon): Boolean {
        return propertiesBlacklist.map {
            PokemonProperties.parse(it)
        }.none {
            it.matches(pokemon)
        }
    }

}