package dev.roanoke.betterbreeding.utils

import com.cobblemon.mod.common.api.properties.CustomPokemonPropertyType
import dev.roanoke.rib.Rib

class BreedablePropertyType(
    override val keys: Iterable<String>,
    override val needsKey: Boolean
) : CustomPokemonPropertyType<BreedableProperty> {

    override fun examples(): Collection<String> {
        return listOf(
            "yes",
            "true",
            "false",
            "no"
        )
    }

    override fun fromString(value: String?): BreedableProperty? {
        value?.let { string ->
            if (string == "true" || string == "yes") {
                return BreedableProperty(breedable=true)
            }
            if (string == "false" || string == "no") {
                return BreedableProperty(breedable=false)
            }
        }
        return null
    }
}