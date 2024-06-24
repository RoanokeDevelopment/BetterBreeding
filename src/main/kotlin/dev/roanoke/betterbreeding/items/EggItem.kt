package dev.roanoke.betterbreeding.items

import com.cobblemon.mod.common.Cobblemon
import dev.roanoke.betterbreeding.breeding.EggInfo
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text

object EggItem {

    fun isEgg(item: ItemStack): Boolean {
        return (item.nbt?.contains("species") ?: false) && item.isOf(Items.TURTLE_EGG)
    }

    fun getEggInfo(item: ItemStack): EggInfo? {
        return item.nbt?.let {
            EggInfo.fromNbt(it)
        }
    }

    fun tryTickEgg(stack: ItemStack, player: ServerPlayerEntity) {

        if (!EggItem.isEgg(stack)) return;
        if (stack.nbt == null) return;

        val timer = stack.nbt?.getInt("timer") ?: 100
        stack.orCreateNbt.putInt("timer", timer - 1)

        val party = Cobblemon.storage.getParty(player.uuid)
        for (i in 0..5) {
            val pokemon = party.get(i)
            // Looking for an ability reducing hatching time in party
            val incubator = pokemon?.let {
                val ability = it.ability.template.name
                (ability == "magmaarmor" || ability == "flamebody" || ability == "steamengine")
            } ?: false
            if (incubator) {
                stack.getOrCreateNbt().putInt("timer", timer - 2)
                break
            }
        }

        if (timer <= 0) {
            val info = stack.nbt?.let { EggInfo.fromNbt(it) }
            if (info != null) {
                stack.removeSubNbt("species")
                stack.decrement(1)

                val pokemon = info.getPokemon()

                party.add(pokemon)

                player.sendMessage(Text.literal("Your Egg hatched!"))
            }
        }
    }

}