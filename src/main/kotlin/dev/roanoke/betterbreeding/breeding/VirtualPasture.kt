package dev.roanoke.betterbreeding.breeding

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.util.giveOrDropItemStack
import com.cobblemon.mod.common.util.isInBattle
import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import dev.roanoke.betterbreeding.BetterBreeding
import dev.roanoke.betterbreeding.items.EggItem
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
    var egg: EggInfo?,
    var ticksTilCheck: Int = 12000
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

    fun getHutchItem(player: ServerPlayerEntity): GuiElementBuilder {
        var element = BetterBreeding.GUIs.getItemOrDefault("emptyEggHutchItem").gbuild()
        if (egg != null) { element = BetterBreeding.GUIs.getItemOrDefault("filledEggHutchItem").gbuild() }

        return element.setCallback { _, _, _ ->
            if (egg != null) {
                player.giveOrDropItemStack(EggItem.getEggItem(egg!!), true)
                egg = null;
                player.sendMessage(BetterBreeding.MESSAGES.getDisplayMessage("action.claimed_egg_from_hutch"))
            } else {
                player.sendMessage(BetterBreeding.MESSAGES.getDisplayMessage("message.hutch_empty"))
            }
        }
    }

    fun openPokeSwapperPastureGui(player: ServerPlayerEntity) {

        if (player.isInBattle()) {
            player.sendMessage(BetterBreeding.MESSAGES.getDisplayMessage("error.no_pasture_in_battle"))
            return
        }

        val cGui: ConfiguredGUI = BetterBreeding.GUIs.getGui("virtual_pasture_pokeswapper") ?: return
        val party = Cobblemon.storage.getParty(player.uuid)

        val gui = cGui.getGui(
            player = player,
            elements = mapOf(
                "X" to pokemon.map {
                    if (it == null) {
                        BetterBreeding.GUIs.getItemOrDefault("defaultElementFillItem").gbuild()
                    } else {
                        GuiElementBuilder.from(GuiUtils.getPokemonGuiElement(it))
                            .setCallback { _, _, _ ->
                                pokemon.remove(it)
                                party.add(it)
                                BetterBreeding.PASTURES.savePasture(this)
                                player.sendMessage(BetterBreeding.MESSAGES.getDisplayMessage("action.retrieved_pokemon_from_daycare"))
                                openPokeSwapperPastureGui(player)
                            }
                    }
                },
                "P" to party.map { GuiElementBuilder.from(GuiUtils.getPokemonGuiElement(it))
                    .setCallback { _, _, _ ->
                        if (pokemon.filterNotNull().size > 1) {
                            player.sendMessage(BetterBreeding.MESSAGES.getDisplayMessage("error.no_room_in_pasture"))
                        } else {
                            if (party.remove(it)) {
                                val indexToReplace = pokemon.indexOfFirst { pmon -> pmon == null }
                                if (indexToReplace == -1) {
                                    pokemon.add(it)
                                } else {
                                    pokemon.set(indexToReplace, it)
                                }
                                BetterBreeding.PASTURES.savePasture(this)
                                openPokeSwapperPastureGui(player)
                                player.sendMessage(BetterBreeding.MESSAGES.getDisplayMessage("action.put_pokemon_in_daycare"))
                            } else {
                                player.sendMessage(Text.literal("You don't even have that Pokemon anymore..."))
                            }
                        }
                    }},
                "I" to listOf(BetterBreeding.GUIs.getItemOrDefault("pasturePokeSwapperInfoItem").gbuild()),
                "E" to listOf(getHutchItem(player))
            ),
            onClose = {
                openPastureGui(player)
            }
        )

        gui.title = BetterBreeding.MESSAGES.getDisplayMessage("gui.pasture_menu.title")

        gui.open()
    }

    fun openPastureGui(player: ServerPlayerEntity) {

        if (player.isInBattle()) {
            player.sendMessage(BetterBreeding.MESSAGES.getDisplayMessage("error.no_pasture_in_battle"))
            return
        }

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
                "I" to listOf(BetterBreeding.GUIs.getItemOrDefault("pastureInfoItem").gbuild()
                    .setCallback { _, _, _ ->
                        openPokeSwapperPastureGui(player)
                    }),
                "E" to listOf(getHutchItem(player)
                )
            )
        )

        gui.title = BetterBreeding.MESSAGES.getDisplayMessage("gui.pasture_menu.title")

        gui.open()

    }

}