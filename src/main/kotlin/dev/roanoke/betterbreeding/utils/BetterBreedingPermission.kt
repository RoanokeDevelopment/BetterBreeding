package dev.roanoke.betterbreeding.utils

import com.cobblemon.mod.common.api.permission.Permission
import com.cobblemon.mod.common.api.permission.PermissionLevel
import dev.roanoke.betterbreeding.BetterBreeding
import net.minecraft.util.Identifier

data class BetterBreedingPermission(
    private val node: String,
    override val level: PermissionLevel
) : Permission {

    override val identifier = Identifier(BetterBreeding.MODID, this.node)

    override val literal = "${BetterBreeding.MODID}.${this.node}"
}
