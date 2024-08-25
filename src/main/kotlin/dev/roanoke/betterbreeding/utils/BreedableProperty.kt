package dev.roanoke.betterbreeding.utils

import com.cobblemon.mod.common.api.properties.CustomPokemonProperty
import com.cobblemon.mod.common.pokemon.Pokemon
import dev.roanoke.rib.Rib

class BreedableProperty(
    var breedable: Boolean = true
): CustomPokemonProperty {

    override fun apply(pokemon: Pokemon) {
        pokemon.persistentData.putBoolean("breedable", breedable)
    }

    override fun asString(): String {
        return if (breedable) {
            "breedable"
        } else {
            "unbreedable"
        }
    }

    override fun matches(pokemon: Pokemon): Boolean {

        if (pokemon.persistentData.contains("breedable")) {
            val isBreedable = pokemon.persistentData.getBoolean("breedable")
            return isBreedable
        } else {
            return true
        }
    }

}