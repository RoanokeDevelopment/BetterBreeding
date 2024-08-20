package dev.roanoke.betterbreeding.utils

import com.cobblemon.mod.common.api.permission.Permission
import com.cobblemon.mod.common.api.permission.PermissionLevel

object BetterBreedingPermissions {

    private const val COMMAND_PREFIX = "command."
    private val permissions = arrayListOf<Permission>()

    val FORCE_BREED = create("${COMMAND_PREFIX}forcebreed", PermissionLevel.CHEAT_COMMANDS_AND_COMMAND_BLOCKS)
    val FORCE_HATCH = create("${COMMAND_PREFIX}forcehatch", PermissionLevel.CHEAT_COMMANDS_AND_COMMAND_BLOCKS)
    val EGG_INFO = create("${COMMAND_PREFIX}egginfo", PermissionLevel.CHEAT_COMMANDS_AND_COMMAND_BLOCKS)
    val OPEN_DAY_CARE = create("${COMMAND_PREFIX}opendaycare", PermissionLevel.CHEAT_COMMANDS_AND_COMMAND_BLOCKS)
    val DAY_CARE = create("${COMMAND_PREFIX}daycare", PermissionLevel.CHEAT_COMMANDS_AND_COMMAND_BLOCKS)
    val NEUTER = create("${COMMAND_PREFIX}neuter", PermissionLevel.CHEAT_COMMANDS_AND_COMMAND_BLOCKS)
    val RELOAD = create("${COMMAND_PREFIX}reload", PermissionLevel.CHEAT_COMMANDS_AND_COMMAND_BLOCKS)

    fun all(): Iterable<Permission> = permissions

    private fun create(node: String, level: PermissionLevel): Permission {
        val permission = BetterBreedingPermission(node, level)
        permissions += permission
        return permission
    }

}