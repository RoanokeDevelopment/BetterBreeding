package dev.roanoke.kotlinfabrictemplate.utils

import com.cobblemon.mod.common.api.permission.Permission
import com.cobblemon.mod.common.api.permission.PermissionLevel
import dev.roanoke.kotlinfabrictemplate.KotlinFabricTemplate
import net.minecraft.util.Identifier

data class ExamplePermission(
    private val node: String,
    override val level: PermissionLevel
) : Permission {

    override val identifier = Identifier(KotlinFabricTemplate.MODID, this.node)

    override val literal = "${KotlinFabricTemplate.MODID}.${this.node}"
}
