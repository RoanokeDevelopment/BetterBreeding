package dev.roanoke.betterbreeding.mixins;

import com.cobblemon.mod.common.block.entity.PokemonPastureBlockEntity;
import dev.roanoke.betterbreeding.api.PastureDataProvider;
import dev.roanoke.betterbreeding.pastures.real.RealPastureData;
import dev.roanoke.betterbreeding.pastures.real.RealPastureManager;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({PokemonPastureBlockEntity.class})
public abstract class PastureBlockEntityMixin implements PastureDataProvider {

    @Unique
    public RealPastureData pastureData;

    @Override
    public RealPastureData getPastureData() {
        if (pastureData == null) {
            pastureData = RealPastureData.Companion.fromPastureEntity((PokemonPastureBlockEntity) (Object) this);
        }
        return pastureData;
    }

    @Override
    public void setPastureData(RealPastureData data) {
        pastureData = data;
    }


    @Inject(
        at = {@At("HEAD")},
        method = {"TICKER$lambda$11"}
    )
    private static void init(World world, BlockPos pos, BlockState state, PokemonPastureBlockEntity pastureEntity, CallbackInfo ci) {

        @SuppressWarnings("unchecked")
        PastureDataProvider provider = (PastureDataProvider) (Object) pastureEntity;
        RealPastureManager.INSTANCE.onTick((ServerWorld) world, pos, state, pastureEntity, provider.getPastureData());

    }

    @Inject(at = @At("HEAD"), method = "writeNbt")
    private void writeNbt(NbtCompound nbt, CallbackInfo ci) {
        if (pastureData != null) {
            pastureData.toNbt(nbt);
        }
    }

    @Inject(at = @At("TAIL"), method = "readNbt")
    private void readNbt(NbtCompound nbt, CallbackInfo ci) {
        pastureData = RealPastureData.Companion.fromNbtOrPasture(nbt, (PokemonPastureBlockEntity)(Object)this);
    }

}