package dev.roanoke.betterbreeding.items

import dev.roanoke.betterbreeding.breeding.EggInfo
import net.minecraft.item.ItemStack

object EggItem {

    fun isEgg(item: ItemStack): Boolean {
        return item.nbt?.contains("species") ?: false
    }

    fun getEggInfo(item: ItemStack): EggInfo? {
        return item.nbt?.let {
            EggInfo.fromNbt(it)
        }
    }

}