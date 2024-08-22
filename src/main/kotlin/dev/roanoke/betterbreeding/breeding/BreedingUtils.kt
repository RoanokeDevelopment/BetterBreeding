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
 */

// Original Source: https://gitlab.com/ludichat/Cobbreeding

package dev.roanoke.betterbreeding.breeding

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.CobblemonItems
import com.cobblemon.mod.common.api.Priority
import com.cobblemon.mod.common.api.abilities.Abilities
import com.cobblemon.mod.common.api.moves.BenchedMove
import com.cobblemon.mod.common.api.moves.MoveTemplate
import com.cobblemon.mod.common.api.moves.Moves
import com.cobblemon.mod.common.api.pokeball.PokeBalls
import com.cobblemon.mod.common.api.pokemon.Natures
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies.getByName
import com.cobblemon.mod.common.api.pokemon.egg.EggGroup
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.block.entity.PokemonPastureBlockEntity
import com.cobblemon.mod.common.pokemon.*
import dev.roanoke.betterbreeding.BetterBreeding
import dev.roanoke.betterbreeding.utils.BreedableProperty
import dev.roanoke.rib.Rib
import net.minecraft.item.Item
import kotlin.math.abs
import kotlin.random.Random
import kotlin.random.nextUInt

object BreedingUtils {

        /**
     * Gets the Pokémon instances from the list of tethered Pokémon.
     * @return Pokémon in the pasture.
     */
    @JvmStatic
    fun List<PokemonPastureBlockEntity.Tethering>.getPokemon(): List<Pokemon?> = this.map { it.getPokemon() }

    /**
     * Converts IVs to an integer array.
     * @return IVs as int array.
     */
    @JvmStatic
    fun IVs.toIntArray(): IntArray = setOf(
        Stats.HP,
        Stats.ATTACK,
        Stats.DEFENCE,
        Stats.SPECIAL_ATTACK,
        Stats.SPECIAL_DEFENCE,
        Stats.SPEED
    ).map { this[it] ?: 0 }.toIntArray()

    /**
     * Converts integer array to IVs.
     * @return int array as IVs.
     */
    @JvmStatic
    fun IntArray.toIVs(): IVs = IVs().apply {
        setOf(
            Stats.HP,
            Stats.ATTACK,
            Stats.DEFENCE,
            Stats.SPECIAL_ATTACK,
            Stats.SPECIAL_DEFENCE,
            Stats.SPEED
        ).zip(this@toIVs.toList()).forEach { (stat, iv) -> this[stat] = iv }
    }

    /**
     * Convert set of moves to int array
     * @return moves as int array.
     */
    @JvmStatic
    fun Set<MoveTemplate>.toIdArray(): IntArray = this.map { it.num }.toIntArray()

    /**
     * Convert set of moves to int array
     * @return moves as int array.
     */
    @JvmStatic
    fun IntArray.toMoves(): Set<MoveTemplate> = this.map { Moves.getByNumericalId(it)!! }.toSet()

    /**
     * Gets a random Pokémon from a pair of Pokémon.
     * @return Random Pokémon from the pair.
     */
    private fun Pair<Pokemon, Pokemon>.random(): Pokemon = if (Random.nextInt(2) == 0) first else second

    /**
     * Applies the effect of the Mirror Herb to Pokemon holding it.
     */
    @JvmStatic
    fun applyMirrorHerb(pokemon: List<Pokemon?>)
    {
        // Finding pokemon in the pasture block holding a Mirror Herb
        for (holder in pokemon)
        {
            if (holder != null && holder.heldItem().item == CobblemonItems.MIRROR_HERB)
            {
                // If a Pokemon is holding a Mirror Herb, we must check every other Pokemon is the pasture to gather
                // Eggmoves the holder can learn
                val possibleEggMoves = getBaby(holder).moves.eggMoves
                for (other in pokemon)
                {
                    if (other != null)
                    {
                        for (accessibleMove in other.allAccessibleMoves) {
                            if (possibleEggMoves.contains(accessibleMove))
                                holder.benchedMoves.add(BenchedMove(accessibleMove, 0))
                        }
                    }
                }
            }
        }
    }

    /**
     * Choose an egg from the possible eggs in the pasture.
     * @return Integer array of the Pokédex number and the computed IVs for the egg.
     */
    @JvmStatic
    fun chooseEgg(pokemon: List<Pokemon?>): EggInfo? {
        val eggs = getPossibleEggs(pokemon.filterNotNull())

        return if (eggs.isNotEmpty()) {
            val entry = eggs.random()

            val form = entry.key

            val couples = entry.value
            val parents = couples.random()
            val stats = calcStats(parents)
            val nature = calcNature(parents)
            val eggMoves = calcEggMoves(parents, form)
            val ability = calcAbility(form, parents)
            val pokeball = calcBall(parents)
            val shiny = calcShiny(parents)

            Rib.LOGGER.info(
                """
                Spawning egg:
                - Species: ${form.species.name}
                - Aspects: [${form.aspects.joinToString()}]
                - Nature: ${nature.name.path}
                - Ability: ${ability}
                - IVs: [${stats.joinToString()}]
                - Egg moves: [${eggMoves.joinToString { it.name }}]
                """.trimIndent()
            )

            EggInfo(
                form.species,
                stats,
                nature,
                form,
                eggMoves,
                ability,
                pokeball,
                shiny
            )
        } else {
            null
        }
    }

    /**
     * @return Collection of all currently possible eggs in the pasture, mapping species to possible parents.
     */
    @JvmStatic
    fun getPossibleEggs(pokemonGroup: List<Pokemon>): Collection<Map.Entry<FormData, List<Pair<Pokemon, Pokemon>>>> {
        val eggs = HashMap<FormData, MutableList<Pair<Pokemon, Pokemon>>>()

        val pokemon = pokemonGroup.filter {
            Rib.LOGGER.info("Filtering Pokemon from Group... is it breedable? ${BreedableProperty().matches(it)}")
            BreedableProperty().matches(it)
        }

        for (i in pokemon.indices) {
            for (j in i + 1 until pokemon.size) {
                val pokemon1 = pokemon[i]
                val species1 = pokemon1.species
                val eggGroups1 = species1.eggGroups

                val pokemon2 = pokemon[j]
                val species2 = pokemon2.species
                val eggGroups2 = species2.eggGroups

                // Undiscovered egg group checks
                if (!eggGroups1.contains(EggGroup.UNDISCOVERED) && !eggGroups2.contains(EggGroup.UNDISCOVERED)) {
                    // Ditto checks
                    if (eggGroups1.contains(EggGroup.DITTO)) {
                        dittoBreed(pokemon2)?.let {
                            if (eggs.containsKey(it)) eggs[it]?.add(Pair(pokemon2, pokemon1))
                            else eggs[it] = mutableListOf(Pair(pokemon2, pokemon1))
                        }
                    } else if (eggGroups2.contains(EggGroup.DITTO)) {
                        dittoBreed(pokemon1)?.let {
                            if (eggs.containsKey(it)) eggs[it]?.add(Pair(pokemon1, pokemon2))
                            else eggs[it] = mutableListOf(Pair(pokemon1, pokemon2))
                        }
                    }
                    // Normal checks
                    else if (eggGroups1.any(eggGroups2::contains)) {
                        if (pokemon1.gender == Gender.FEMALE && pokemon2.gender == Gender.MALE) {
                            getBaby(pokemon1).let {
                                if (eggs.containsKey(it)) eggs[it]?.add(Pair(pokemon2, pokemon1))
                                else eggs[it] = mutableListOf(Pair(pokemon2, pokemon1))
                            }
                        } else if (pokemon2.gender == Gender.FEMALE && pokemon1.gender == Gender.MALE) {
                            getBaby(pokemon2).let {
                                if (eggs.containsKey(it)) eggs[it]?.add(Pair(pokemon1, pokemon2))
                                else eggs[it] = mutableListOf(Pair(pokemon1, pokemon2))
                            }
                        }
                    }
                }
            }
        }

        // Nidoran family and Volbeat/Illumise produce both male and female eggs despite being different Pokémon
        // If the egg list contains a female Nidoran, we must add male Nidoran to it, and the other way around
        if (eggs.containsKey(getByName("nidoranf")!!.standardForm)) {
            eggs[getByName("nidoranm")!!.standardForm] = eggs[getByName("nidoranf")!!.standardForm]!!
        } else if (eggs.containsKey(getByName("nidoranm")!!.standardForm)) {
            // Avoiding adding female Nidoran if we just added male Nidoran due to the previous condition
            eggs[getByName("nidoranf")!!.standardForm] = eggs[getByName("nidoranm")!!.standardForm]!!
        }

        // Doing the same thing for Volbeat and Illumise
        if (eggs.containsKey(getByName("illumise")!!.standardForm)) {
            eggs[getByName("volbeat")!!.standardForm] = eggs[getByName("illumise")!!.standardForm]!!
        } else if (eggs.containsKey(getByName("volbeat")!!.standardForm)) {
            eggs[getByName("illumise")!!.standardForm] = eggs[getByName("volbeat")!!.standardForm]!!
        }

        return eggs.mapValues { (_, v) -> v.toList() }.entries
    }

    private fun dittoBreed(other: Pokemon): FormData? {
        return if (other.species == getByName("manaphy")) {
            // Manaphy => Phione check
            getByName("phione")!!.standardForm
        } else if (other.species.eggGroups.contains(EggGroup.DITTO)) {
            // Must be breedable / not another Ditto
            null
        } else {
            getBaby(other)
        }
    }

    private fun getBaby(pokemon: Pokemon): FormData {
        // Changed how this works until Cobblemon fixes getting the right form of pre-evolutions
        var species = pokemon.species
        var form = pokemon.form
         while (species.preEvolution != null)
         {
             species = species.preEvolution!!.species
         }

        // regional-exclusive evolution edge case
        if (pokemon.species.name == "Perrserker" ||
            pokemon.species.name == "Sirfetch'd" ||
            pokemon.species.name == "Mr. Rime" ||
            pokemon.species.name == "Cursola" ||
            pokemon.species.name == "Obstagoon" ||
            pokemon.species.name == "Runerigus" ||
            pokemon.species.name == "Clodsire" ||
            pokemon.species.name == "Overqwil" ||
            pokemon.species.name == "Sneasler")
            return species.forms[1]

        return species.forms.find { it.formOnlyShowdownId().contains(form.formOnlyShowdownId()) } ?:
               species.standardForm

        //var baby = pokemon.form

        //while (baby.preEvolution != null) {
        //    baby = baby.preEvolution!!.form
        //}

        //return baby
    }

    private fun getRealIV(pokemon: Pokemon, stat: Stats): Int {
        val key = "bc_${stat.name.lowercase()}".trim()

        // if doesn't have bottlecaps key, then we just return the IV directly
        if (!pokemon.persistentData.contains(key)) {
            return pokemon.ivs[stat]!!
        }

        return abs(pokemon.persistentData.getInt("bc_${key}"))
    }

    private fun calcStats(parents: Pair<Pokemon, Pokemon>): IVs {
        val finalStats = IVs.createRandomIVs()
        val (father, mother) = parents
        val ivs = mutableSetOf(
            Stats.HP,
            Stats.ATTACK,
            Stats.DEFENCE,
            Stats.SPECIAL_ATTACK,
            Stats.SPECIAL_DEFENCE,
            Stats.SPEED
        )

        // First transferred IV
        // Check parents for power items
        val ivsToChooseFrom = mapOf(
            Pair(father, powerItemToIV(father.heldItem().item)),
            Pair(mother, powerItemToIV(mother.heldItem().item))
        ).filterNot { it.value == null }

        var iv: Stats
        var parent: Pokemon
        if (ivsToChooseFrom.isEmpty()) {
            // No power item
            iv = ivs.random()
            parent = parents.random()
        } else {
            val (pokemon, stat) = ivsToChooseFrom.toList().random()
            iv = stat!!
            parent = pokemon
        }

        parent.ivs[iv]?.let {
            finalStats[iv] = getRealIV(parent, iv)
        }

        ivs.remove(iv)

        var ivCount =
            if (father.heldItem().item == CobblemonItems.DESTINY_KNOT || mother.heldItem().item == CobblemonItems.DESTINY_KNOT) 4
            else 2
        while (ivCount != 0) {
            iv = ivs.random()
            parent = parents.random()

            parent.ivs[iv]?.let {
                finalStats[iv] = getRealIV(parent, iv)
            }

            ivs.remove(iv)

            ivCount--
        }

        return finalStats
    }

    private fun powerItemToIV(item: Item): Stats? = when (item) {
        CobblemonItems.POWER_WEIGHT -> Stats.HP
        CobblemonItems.POWER_BRACER -> Stats.ATTACK
        CobblemonItems.POWER_BELT -> Stats.DEFENCE
        CobblemonItems.POWER_LENS -> Stats.SPECIAL_ATTACK
        CobblemonItems.POWER_BAND -> Stats.SPECIAL_DEFENCE
        CobblemonItems.POWER_ANKLET -> Stats.SPEED
        // If the parent isn't holding an item, return null to use in logic
        else -> null
    }

    private fun calcNature(parents: Pair<Pokemon, Pokemon>): Nature {
        val (father, mother) = parents
        val parentsItems = mapOf(
            Pair(father, father.heldItem().item),
            Pair(mother, mother.heldItem().item)
        ).filterNot { it.value != CobblemonItems.EVERSTONE }

        if (parentsItems.isEmpty())
            return Natures.getRandomNature()
        return parentsItems.toList().random().first.nature
    }

    private fun calcEggMoves(parents: Pair<Pokemon, Pokemon>, child: FormData): Set<MoveTemplate> {
        val possibleEggMoves = child.moves.eggMoves
        val eggMoves = mutableSetOf<MoveTemplate>()

        for (parent in parents.toList()) {
            for (accessibleMove in parent.allAccessibleMoves) {
                if (possibleEggMoves.contains(accessibleMove))
                    eggMoves.add(accessibleMove)
            }

            // TODO Special condition for Pichu learning Volt Tackle when the Light Ball is implemented
            if (child.name == "Pichu")
                eggMoves.add(Moves.getByName("volttackle")!!)
        }

        return eggMoves
    }

    private fun calcAbility(form: FormData, parents: Pair<Pokemon, Pokemon>): String {
        val (father, mother) = parents

        // Get parent to pass ability down (either mother or non-ditto parent).
        val ancestor =
            if (mother.species == getByName("ditto")) father
            else mother
        val oldAbility = ancestor.ability

        // Keep old ability if it was forced, otherwise find ability.
        // If forced abilities are disabled in the config, then ignore the forced ability and do as if it wasn't forced.
        return if (oldAbility.forced) oldAbility.name
        else {
            // Get priority and index. If not defined, look them up.
            val (priority, index) = if (oldAbility.index >= 0) Pair(oldAbility.priority, oldAbility.index)
            else {
                // If the indices can't be found here for some reason, default to the 1st common ability.
                val entry = ancestor.form.abilities.mapping.entries.firstOrNull { (_, abilities) ->
                    abilities.map { it.template }.contains(oldAbility.template)
                }
                entry?.run {
                    Pair(key, value.map { it.template }.indexOf(oldAbility.template))
                } ?: Pair(Priority.LOWEST, 0)
            }

            // Get proper ability and other abilities.
            // If the index can't be found, it's likely an evolved parent having kept index 1 despite only having one ability option.
            // If hidden abilities are disabled by config, then filter them out of the remaining abilities.
            val inheritedAbility = form.abilities.mapping[priority]?.getOrNull(index)?.template ?: // The same ability slot
                                   form.abilities.mapping[priority]?.firstOrNull()?.template ?:  // If it can't be found, the first slot of the same priority
                                   form.abilities.mapping[Priority.LOWEST]?.firstOrNull()?.template ?:  // If it can't be found, the first slot of the lowest priority
                                   Abilities.DUMMY  // Basically the worst case scenario
            val remainingAbilities =
                if (true) // Cobbreeding.config.hiddenAbilitiesEnabled
                    form.abilities.mapping.values
                        .flatten()
                        .map { it.template }
                        .filterNot { it == inheritedAbility }
                else
                    form.abilities.mapping.values
                        .flatten()
                        .filterNot { it.priority == Priority.LOW }
                        .map { it.template }
                        .filterNot { it == inheritedAbility }

            val chance = when (priority) {
                // Keep hidden ability: 60%
                Priority.LOW -> 6u
                // Keep other ability: 80%
                else -> 8u
            }

            // Select if the inherited ability should be kept or choose another one.
            val template = if (remainingAbilities.isEmpty() || Random.nextUInt(10u) < chance) inheritedAbility
            else remainingAbilities.random()

            // Create the ability as not forced.
            template.name
        }
    }

    private fun calcBall(parents: Pair<Pokemon, Pokemon>) : String
    {
        var ball = parents.second.caughtBall // Mother ball by default
        // If both parents are of the same species, the child inherit from either one at random
        if (parents.first.species.name == parents.second.species.name)
        ball =  parents.random().caughtBall
        // Not inheriting ball from Ditto
        if (parents.second.species.name == "Ditto")
            ball =  parents.first.caughtBall

        // Cherish Ball, Master Ball, and Strange Ball (unimplemented) count as regular pokeball
        if (ball == PokeBalls.CHERISH_BALL || ball == PokeBalls.MASTER_BALL)
            ball = PokeBalls.POKE_BALL
        return ball.name.toString()
    }

    private fun rollOdds(shinyOdds: Float): Boolean {
        return if (shinyOdds < 1) true else Random.nextInt(0, shinyOdds.toInt()) == 0
    }

    private fun calcShiny(parents: Pair<Pokemon, Pokemon>): Boolean?
    {
        val shinyOdds = Cobblemon.config.shinyRate
        val shinyMultiplier = BetterBreeding.CONFIG.shinyMultiplier

        return when (BetterBreeding.CONFIG.shinyMethod) {
            "masuda" -> {
                if (parents.first.originalTrainer != parents.second.originalTrainer) {
                    rollOdds(shinyOdds / shinyMultiplier)
                } else {
                    rollOdds(shinyOdds)
                }
            }
            "crystal" -> {
                var crystalOdds = shinyOdds

                parents.toList().forEach {
                    if (it.shiny) {
                        crystalOdds /= shinyMultiplier
                    }
                }

                return rollOdds(crystalOdds)
            }
            "crystal masuda" -> {
                var crystalmasudaOdds = shinyOdds

                parents.toList().forEach {
                    if (it.shiny) {
                        crystalmasudaOdds /= shinyMultiplier
                    }
                }

                if (parents.first.originalTrainer != parents.second.originalTrainer) {
                    crystalmasudaOdds /= shinyMultiplier
                }

                return rollOdds(crystalmasudaOdds)
            }
            else -> {
                rollOdds(shinyOdds)
            }
        }
    }
}
