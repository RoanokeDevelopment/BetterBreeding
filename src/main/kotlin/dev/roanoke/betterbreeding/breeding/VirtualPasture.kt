package dev.roanoke.betterbreeding.breeding

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.util.giveOrDropItemStack
import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import dev.roanoke.betterbreeding.BetterBreeding
import dev.roanoke.rib.gui.configurable.ConfiguredGUI
import dev.roanoke.rib.utils.GuiUtils
import dev.roanoke.rib.utils.ItemBuilder
import eu.pb4.sgui.api.elements.GuiElementBuilder
import net.minecraft.item.Items
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import java.util.UUID

class VirtualPasture(
    val player: UUID,
    val pokemon: MutableList<Pokemon?>,
    var egg: EggInfo?
) {

    companion object {
        fun fromJson(json: JsonObject): VirtualPasture {
            val playerUUID = UUID.fromString(json.get("player").asString)
            val pokemonsArray = json.getAsJsonArray("pokemon")
            val pokemons: MutableList<Pokemon?> = mutableListOf()

            for (jsonElement in pokemonsArray) {
                if (jsonElement.isJsonObject) {
                    pokemons.add(Pokemon.loadFromJSON(jsonElement.asJsonObject))
                } else {
                    // Assuming the null case is handled correctly by JsonNull
                    pokemons.add(null)
                }
            }

            val eggJson = json.get("egg")
            val egg = if (eggJson != null && !eggJson.isJsonNull) EggInfo.fromJson(eggJson.asJsonObject) else null

            return VirtualPasture(playerUUID, pokemons, egg)
        }
    }


    fun toJson(): JsonObject {
        val json = JsonObject()
        json.addProperty("player", player.toString())
        val pokemonsJson = JsonArray()
        pokemon.forEach {
            if (it != null) pokemonsJson.add(it.saveToJSON(JsonObject()))
            else pokemonsJson.add(JsonNull.INSTANCE) // adding null value in the array if the item is null
        }
        json.add("pokemon", pokemonsJson)
        json.add("egg", egg?.toJson() ?: JsonNull.INSTANCE)
        return json
    }

    fun openPastureGui(player: ServerPlayerEntity) {

        val cGui: ConfiguredGUI = BetterBreeding.GUIs.getGui("virtual_pasture") ?: return

        val gui = cGui.getGui(
            player = player,
            elements = mapOf(
                "X" to pokemon.map {
                    if (it == null) {
                        GuiElementBuilder.from(Items.GRAY_STAINED_GLASS.defaultStack)
                    } else {
                        GuiElementBuilder.from(GuiUtils.getPokemonGuiElement(it))
                    }
                },
                "I" to listOf(GuiElementBuilder.from(ItemBuilder(Items.BEEHIVE)
                    .setCustomName(Text.literal("The Birds and the Bees"))
                    .addLore(listOf(
                        Text.literal(""),
                        Text.literal("Leave two compatible Pokemon in here for a while"),
                        Text.literal("When you come back, you may find an Egg...")
                    ))
                    .build())),
                "E" to listOf(GuiElementBuilder.from(ItemBuilder(Items.COMPOSTER)
                    .setCustomName(Text.literal("Egg Hutch"))
                    .addLore(listOf(
                        if (egg != null)
                            Text.literal("Click to collect your Egg!")
                        else
                            Text.literal("There isn't an Egg in here, yet...")
                    ))
                    .build())
                    .setCallback { _, _, _ ->
                        if (egg != null) {
                            player.giveOrDropItemStack(egg!!.getEggItem(), true)
                            egg = null;
                            player.sendMessage(Text.literal("You found an Egg in the hutch!"))
                        } else {
                            player.sendMessage(Text.literal("There's nothing in there..."))
                        }
                    }
                )
            )
        )

        gui.title = Text.literal("Roanoke Day Care")

        gui.open()

    }

}