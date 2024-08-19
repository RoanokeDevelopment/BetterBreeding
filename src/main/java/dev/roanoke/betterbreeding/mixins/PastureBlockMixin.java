package dev.roanoke.betterbreeding.mixins;

import com.cobblemon.mod.common.block.PastureBlock;
import com.cobblemon.mod.common.block.entity.PokemonPastureBlockEntity;
import dev.roanoke.betterbreeding.pastures.real.RealPastureManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({PastureBlock.class})
public abstract class PastureBlockMixin {

    @Inject(
        at = {@At("HEAD")},
        method = {"onBreak"}
    )
    private void onBroken(World world, BlockPos pos, BlockState state, PlayerEntity player, CallbackInfo ci) {
        if (world instanceof ServerWorld && player instanceof ServerPlayerEntity) {
            System.out.println("Starting Block Broken, getting this, should get more output..");
            PastureBlock thisObject = (PastureBlock)(Object)this;
            BlockPos basePos = thisObject.getBasePosition(state, pos);
            BlockEntity be = world.getBlockEntity(basePos);
            System.out.println("Pasture Block Broken at " + pos.toString());
            if (be instanceof PokemonPastureBlockEntity) {
                RealPastureManager.INSTANCE.onBreak((ServerWorld) world, pos, state, (ServerPlayerEntity) player, (PokemonPastureBlockEntity) be);
            }
        }
    }
}