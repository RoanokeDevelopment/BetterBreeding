package dev.roanoke.kotlinfabrictemplate.utils

import com.cobblemon.mod.common.api.permission.Permission
import com.cobblemon.mod.common.api.permission.PermissionLevel

object ExamplePermissions {

    private const val COMMAND_PREFIX = "command."
    private val permissions = arrayListOf<Permission>()

    val GET_POKE_PASTE = create("${COMMAND_PREFIX}getpokepaste", PermissionLevel.CHEAT_COMMANDS_AND_COMMAND_BLOCKS)

    fun all(): Iterable<Permission> = permissions

    private fun create(node: String, level: PermissionLevel): Permission {
        val permission = ExamplePermission(node, level)
        permissions += permission
        return permission
    }

}