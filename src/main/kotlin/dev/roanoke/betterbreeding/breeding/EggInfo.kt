package dev.roanoke.betterbreeding.breeding

import com.cobblemon.mod.common.api.moves.BenchedMove
import com.cobblemon.mod.common.api.moves.MoveTemplate
import com.cobblemon.mod.common.api.pokemon.Natures
import com.cobblemon.mod.common.api.pokemon.PokemonProperties
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies
import com.cobblemon.mod.common.api.pokemon.feature.FlagSpeciesFeature
import com.cobblemon.mod.common.api.pokemon.feature.StringSpeciesFeature
import com.cobblemon.mod.common.pokemon.*
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

        fun getEggItem(): ItemStack {
            val eggStack: ItemStack = ItemBuilder(Items.TURTLE_EGG)
                .setCustomName(Text.literal("Pokemon Egg"))
                .build()

            this.toNbt(eggStack.orCreateNbt)
            eggStack.orCreateNbt.putInt(
                "timer", floor((species?.eggCycles ?: 20) * 600 * 1.0).toInt() // 1.0 should be hatch multiplier, ?: 20 is egg cycles default
            )

            return eggStack
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