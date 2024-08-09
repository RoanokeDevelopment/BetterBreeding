/*
 * Copyright (c) 2023 Ludichat
 * Copyright (c) 2023 Fuzuki <fuzuki@fuzuki.dev>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Original Source: https://gitlab.com/ludichat/Cobbreeding
 *
 * Additional modifications made by complacentdev/comp@roanoke.network on Saturday 22nd June 2024. These modifications
 * include JSON serialisation, as well as moving some getItem/getPokemon logic into EggInfo
 */

package dev.roanoke.betterbreeding.breeding

import com.cobblemon.mod.common.api.moves.BenchedMove
import com.cobblemon.mod.common.api.moves.MoveTemplate
import com.cobblemon.mod.common.api.pokemon.Natures
import com.cobblemon.mod.common.api.pokemon.PokemonProperties
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies
import com.cobblemon.mod.common.api.pokemon.feature.FlagSpeciesFeature
import com.cobblemon.mod.common.api.pokemon.feature.StringSpeciesFeature
import com.cobblemon.mod.common.pokemon.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import dev.roanoke.betterbreeding.breeding.PastureUtils.toIVs
import dev.roanoke.betterbreeding.breeding.PastureUtils.toIdArray
import dev.roanoke.betterbreeding.breeding.PastureUtils.toIntArray
import dev.roanoke.betterbreeding.breeding.PastureUtils.toMoves
import dev.roanoke.rib.utils.ItemBuilder
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import kotlin.math.floor

data class EggInfo(
        val species: Species?,
        val ivs: IVs?,
        val nature: Nature?,
        val form: FormData?,
        val eggMoves: Set<MoveTemplate>?,
        val ability: String,
        val pokeballName: String?,
        val shiny: Boolean?
    ) {
        enum class Keys(val key: String) {
            Species("species"),
            IVs("ivs"),
            Nature("nature"),
            Form("form"),
            EggMoves("egg_moves"),
            Ability("ability"),
            Pokeball("pokeball"),
            Shiny("shiny"),
        }

        companion object {
            @JvmStatic
            fun fromNbt(nbt: NbtCompound): EggInfo {
                val speciesNbt = nbt.getString(Keys.Species.key)
                val ivsNbt = nbt.getIntArray(Keys.IVs.key)
                val natureNbt = nbt.getString(Keys.Nature.key)
                val formNbt = nbt.getString(Keys.Form.key)
                val eggMovesNbt = nbt.getIntArray(Keys.EggMoves.key)
                val abilityNbt = nbt.getString(Keys.Ability.key)
                val pokeballNbt = nbt.getString(Keys.Pokeball.key)
                val shinyNbt = nbt.getBoolean(Keys.Shiny.key)

                val species = speciesNbt.let { PokemonSpecies.getByName(speciesNbt) }
                    ?: PokemonSpecies.getByPokedexNumber(nbt.getInt(Keys.Species.key))
                return EggInfo(
                    species,
                    ivsNbt?.toIVs(),
                    natureNbt?.let { Natures.getNature(Identifier(it)) },
                    species?.forms?.find { it.formOnlyShowdownId() == formNbt },
                    eggMovesNbt?.toMoves(),
                    abilityNbt,
                    pokeballNbt,
                    shinyNbt
                )
            }
            @JvmStatic
            fun fromJson(json: JsonObject): EggInfo {
                val gson = Gson()
                val species = PokemonSpecies.getByName(json["species"].asString) ?: PokemonSpecies.getByPokedexNumber(json["species"].asInt)
                val ivs = json["ivs"].asJsonArray.map { it.asInt }.toIntArray()
                val nature = json["nature"].asString.let { Natures.getNature(Identifier(it)) }
                val form = species?.forms?.find { it.formOnlyShowdownId() == json["form"].asString }
                val eggMoves = json["eggMoves"].asJsonArray.map { it.asInt }.toIntArray()
                val ability = json["ability"].asString
                val pokeball = json["pokeball"].asString
                val shiny = json["shiny"].asBoolean

                return EggInfo(
                    species, ivs.toIVs(), nature, form, eggMoves.toMoves(), ability, pokeball, shiny
                )
            }
        }

        fun toNbt(nbt: NbtCompound) {
            nbt.putString(Keys.Species.key, species?.showdownId() ?: "")
            nbt.putIntArray(Keys.IVs.key, ivs?.toIntArray() ?: intArrayOf())
            nbt.putString(Keys.Nature.key, nature?.name.toString())
            nbt.putString(Keys.Form.key, form?.formOnlyShowdownId().toString())
            nbt.putIntArray(Keys.EggMoves.key, eggMoves?.toIdArray() ?: intArrayOf())
            nbt.putString(Keys.Ability.key, ability)
            nbt.putString(Keys.Pokeball.key, pokeballName)
            shiny?.let { nbt.putBoolean(Keys.Shiny.key, it) }
        }

        fun toJson(): JsonObject {
            val gson = Gson()
            val json = JsonObject()
            json.addProperty("species", species?.showdownId())
            json.add("ivs", gson.toJsonTree(ivs?.toIntArray()))
            json.addProperty("nature", nature?.name.toString())
            json.addProperty("form", form?.formOnlyShowdownId())
            json.add("eggMoves", gson.toJsonTree(eggMoves?.toIdArray() ?: intArrayOf()))
            json.addProperty("ability", ability)
            json.addProperty("pokeball", pokeballName)
            json.addProperty("shiny", shiny)

            return json
        }

        fun getPokemon(): Pokemon {
            val pokemonProperties = PokemonProperties()

            species?.run {
                pokemonProperties.species = showdownId()
                forms.find { it.formOnlyShowdownId() == form?.formOnlyShowdownId() }?.run {
                    aspects.forEach {
                        // alternative form
                        pokemonProperties.customProperties.add(FlagSpeciesFeature(it, true))
                        // regional bias
                        pokemonProperties.customProperties.add(
                            StringSpeciesFeature(
                                "region_bias",
                                it.split("-").last()
                            )
                        )
                    }
                }
            }
            ivs?.let { pokemonProperties.ivs = it }
            nature?.let { pokemonProperties.nature = it.name.toString() }
            pokemonProperties.ability = ability
            pokeballName?.let { pokemonProperties.pokeball = it }
            pokemonProperties.shiny = shiny

            val pokemon = pokemonProperties.create()

            for (move in eggMoves ?: setOf()) {
                // Always adding the move to benched moves to easily add it in its allAccessibleMoves so
                // the pokemon will be able to transfer its eggmoves to its children anytime
                pokemon.benchedMoves.add(BenchedMove(move, 0))
                if (pokemon.moveSet.hasSpace()) {
                    pokemon.moveSet.add(move.create())
                }
            }

            return pokemon

        }

    }