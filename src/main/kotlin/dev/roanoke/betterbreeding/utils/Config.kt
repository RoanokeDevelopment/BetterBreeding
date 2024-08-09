package dev.roanoke.betterbreeding.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileReader
import java.io.FileWriter

data class Config(
    var eggCheckTicks: Int = 12000,
    var eggCheckChance: Double = 0.5,
    val eggHatchMultiplier: Double = 1.0,
    val shinyMethod: String = "disabled",
    val shinyMultiplier: Float = 4.0f
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
}