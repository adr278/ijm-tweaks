package com.itsjustmiaouss.ijmtweaks.mixin;

import com.itsjustmiaouss.ijmtweaks.config.IJMTweaksConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientWorldMixin {

    @Inject(method = "addDestroyBlockEffect", at = @At("HEAD"), cancellable = true)
    private void inject(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (state.isAir() || !state.shouldSpawnTerrainParticles()) return;

        ClientLevel level = (ClientLevel) (Object) this;
        VoxelShape voxelShape = state.getShape(level, pos);

        int lvl = Mth.clamp(IJMTweaksConfig.get().blockBreakParticle, 0, 4);
        boolean doCull = lvl != 0;

        float linear = (5 - lvl) / 5.0f;
        float baseKeepChance = Mth.clamp((float) Math.pow(linear, 1.35), 0.0f, 1.0f);

        final int[] totalCandidates = new int[] { 0 };
        voxelShape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            double d = Math.min(1.0, maxX - minX);
            double e = Math.min(1.0, maxY - minY);
            double f = Math.min(1.0, maxZ - minZ);

            int i = Math.max(2, Mth.ceil(d / 0.25));
            int j = Math.max(2, Mth.ceil(e / 0.25));
            int k = Math.max(2, Mth.ceil(f / 0.25));

            totalCandidates[0] += i * j * k;
        });

        float sizeT = Mth.clamp((totalCandidates[0] - 8) / 56.0f, 0.0f, 1.0f);
        float sizeBoost = 0.35f * sizeT;
        float keepChance = doCull
                ? (float) Math.pow(baseKeepChance, 1.0f + sizeBoost)
                : 1.0f;

        final RandomSource rand = doCull
                ? RandomSource.create(pos.asLong() ^ 0x9E3779B97F4A7C15L)
                : RandomSource.create(0L);

        voxelShape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            double d = Math.min(1.0, maxX - minX);
            double e = Math.min(1.0, maxY - minY);
            double f = Math.min(1.0, maxZ - minZ);

            int i = Math.max(2, Mth.ceil(d / 0.25));
            int j = Math.max(2, Mth.ceil(e / 0.25));
            int k = Math.max(2, Mth.ceil(f / 0.25));

            for (int l = 0; l < i; ++l) {
                for (int m = 0; m < j; ++m) {
                    for (int n = 0; n < k; ++n) {
                        if (doCull && rand.nextFloat() > keepChance) continue;

                        double g = ((double) l + 0.5) / (double) i;
                        double h = ((double) m + 0.5) / (double) j;
                        double o = ((double) n + 0.5) / (double) k;

                        double p = g * d + minX;
                        double q = h * e + minY;
                        double r = o * f + minZ;

                        Minecraft.getInstance().particleEngine.add(
                                new TerrainParticle(level,
                                        pos.getX() + p,
                                        pos.getY() + q,
                                        pos.getZ() + r,
                                        g - 0.5,
                                        h - 0.5,
                                        o - 0.5, state, pos));
                    }
                }
            }
        });

        ci.cancel();
    }

}
