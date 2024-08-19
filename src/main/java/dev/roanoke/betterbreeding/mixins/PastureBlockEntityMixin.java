package dev.roanoke.betterbreeding.mixins;

import com.cobblemon.mod.common.block.entity.PokemonPastureBlockEntity;
import dev.roanoke.betterbreeding.pastures.RealPastureManager;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({PokemonPastureBlockEntity.class})
public abstract class PastureBlockEntityMixin {

    @Inject(
        at = {@At("HEAD")},
        method = {"TICKER$lambda$11"}
    )
    private static void init(World world, BlockPos pos, BlockState state, PokemonPastureBlockEntity pastureEntity, CallbackInfo ci) {
        //System.out.println("Pasture Block Ticking at " + pos.toString());
        RealPastureManager.INSTANCE.onTick((ServerWorld) world, pos, state, pastureEntity);
    }

}