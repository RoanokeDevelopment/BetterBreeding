package dev.roanoke.betterbreeding.utils

import com.cobblemon.mod.common.api.properties.CustomPokemonPropertyType

class BreedablePropertyType(
    override val keys: Iterable<String>,
    override val needsKey: Boolean
) : CustomPokemonPropertyType<BreedableProperty> {

    override fun examples(): Collection<String> {
        return listOf(
            "breedable=true",
            "breedable=false",
            "breedable",
            "unbreedable"
        )
    }

    fun splitKeyValue(input: String): Pair<String, String>? {
        val parts = input.split("=")
        return if (parts.size == 2) {
            Pair(parts[0], parts[1])
        } else {
            null
        }
    }

    override fun fromString(value: String?): BreedableProperty? {
        value?.let { string ->

            if (string == "breedable") {
                return BreedableProperty().also {
                    it.breedable = true
                }
            }

            if (string == "unbreedable") {
                return BreedableProperty().also {
                    it.breedable = false
                }
            }

            splitKeyValue(string)?.let { (key, value) ->
                val parsedBool = value.toBoolean()
                if (key == "breedable") {
                    return BreedableProperty().also {
                        it.breedable = parsedBool
                    }
                }
            }
        }
        return null
    }
}