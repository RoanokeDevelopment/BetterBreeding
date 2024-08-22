package dev.roanoke.betterbreeding.utils

import com.cobblemon.mod.common.api.properties.CustomPokemonProperty
import com.cobblemon.mod.common.pokemon.Pokemon
import dev.roanoke.rib.Rib

class BreedableProperty: CustomPokemonProperty {

    var breedable: Boolean = true

    override fun apply(pokemon: Pokemon) {
        pokemon.persistentData.putBoolean("breedable", breedable)
    }

    override fun asString(): String {
        return "breedable=${breedable}"
    }

    override fun matches(pokemon: Pokemon): Boolean {

        if (pokemon.persistentData.contains("breedable")) {
            val isBreedable = pokemon.persistentData.getBoolean("breedable")
            Rib.LOGGER.info("Pokemon has breedable data and it is: $isBreedable")
            return isBreedable
        } else {
            Rib.LOGGER.info("Pokemon didnt hav ebreedable data, returning true")
            return true
        }
    }

}