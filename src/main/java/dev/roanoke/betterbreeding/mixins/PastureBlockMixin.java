package dev.roanoke.betterbreeding.mixins;

import com.cobblemon.mod.common.block.PastureBlock;
import com.cobblemon.mod.common.block.entity.PokemonPastureBlockEntity;
import dev.roanoke.betterbreeding.api.PastureDataProvider;
import dev.roanoke.betterbreeding.pastures.real.RealPastureData;
import dev.roanoke.betterbreeding.pastures.real.RealPastureManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({PastureBlock.class})
public abstract class PastureBlockMixin {

    @Inject(
        at = {@At("HEAD")},
        method = {"onBreak"}
    )
    private void onBroken(World world, BlockPos pos, BlockState state, PlayerEntity player, CallbackInfo ci) {
        if (world instanceof ServerWorld && player instanceof ServerPlayerEntity) {
            PastureBlock thisObject = (PastureBlock)(Object)this;
            BlockPos basePos = thisObject.getBasePosition(state, pos);
            BlockEntity be = world.getBlockEntity(basePos);

            if (be instanceof PokemonPastureBlockEntity) {

                if (be instanceof PastureDataProvider) {
                    PastureDataProvider provider = (PastureDataProvider) be;
                    RealPastureData data = provider.getPastureData();

                    RealPastureManager.INSTANCE.onBreak((ServerWorld) world, pos, state, (ServerPlayerEntity) player, (PokemonPastureBlockEntity) be, data);

                }

            }
        }
    }

    @Inject(at = @At("HEAD"), method = "onUse", cancellable = true)
    private void onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        if (world instanceof ServerWorld && player instanceof ServerPlayerEntity) {
            PastureBlock thisObject = (PastureBlock)(Object)this;
            BlockPos basePos = thisObject.getBasePosition(state, pos);
            BlockEntity be = world.getBlockEntity(basePos);

            if (be instanceof PokemonPastureBlockEntity) {
                if (be instanceof PastureDataProvider) {
                    PastureDataProvider provider = (PastureDataProvider) be;
                    RealPastureData data = provider.getPastureData();

                    if (RealPastureManager.INSTANCE.onUse((ServerWorld) world, (ServerPlayerEntity) player, hand, hit, data)) {
                        cir.setReturnValue(ActionResult.FAIL);
                    }

                }

            }
        }
    }

}