package dev.roanoke.betterbreeding.utils

import com.cobblemon.mod.common.api.properties.CustomPokemonProperty
import com.cobblemon.mod.common.pokemon.Pokemon

class BreedableProperty: CustomPokemonProperty {

    var breedable: Boolean = true

    override fun apply(pokemon: Pokemon) {
        pokemon.persistentData.putBoolean("breedable", breedable)
    }

    override fun asString(): String {
        return "breedable=${breedable}"
    }

    override fun matches(pokemon: Pokemon): Boolean {
        return pokemon.persistentData.getBoolean("breedable") ?: true
    }

}