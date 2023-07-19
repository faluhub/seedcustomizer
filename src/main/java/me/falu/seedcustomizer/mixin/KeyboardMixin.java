package me.falu.seedcustomizer.mixin;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.MapLike;
import me.falu.seedcustomizer.core.owner.MinecraftServerDataOwner;
import me.falu.seedcustomizer.core.owner.OverridableBiomeArrayOwner;
import me.falu.seedcustomizer.core.SafeFiles;
import me.falu.seedcustomizer.mixin.access.ChunkTicketManagerAccessor;
import me.falu.seedcustomizer.mixin.access.LevelPropertiesAccessor;
import me.falu.seedcustomizer.mixin.access.ServerChunkManagerAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.datafixer.NbtOps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Util;
import net.minecraft.util.dynamic.RegistryOps;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.registry.RegistryTracker;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeArray;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.level.ServerWorldProperties;
import net.minecraft.world.level.storage.LevelStorage;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Mixin(Keyboard.class)
public abstract class KeyboardMixin {
    @Shadow protected abstract void debugWarn(String string, Object... objects);
    @Shadow @Final private MinecraftClient client;

    @Inject(method = "processF3", at = @At("TAIL"), cancellable = true)
    private void checkCustomKey(int key, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) {
            if (key == GLFW.GLFW_KEY_V) {
                MinecraftServer server = this.client.getServer();
                if (server != null && this.client.world != null) {
                    ServerWorld world = server.getWorld(this.client.world.getRegistryKey());
                    if (world != null) {
                        try {
                            this.regen(world);
                            this.debugWarn("Regenerating all chunks");
                            cir.setReturnValue(true);
                        } catch (Exception e) { e.printStackTrace(); }
                    }
                }
            }
        }
    }

    @Unique
    private void regen(ServerWorld originalWorld) throws Exception {
        Path tempDir = Files.createTempDirectory("RegenProcess");
        LevelStorage levelStorage = LevelStorage.create(tempDir);
        try (LevelStorage.Session session = levelStorage.createSession("RegenProcessTemp")) {
            LevelPropertiesAccessor levelProperties = ((LevelPropertiesAccessor) originalWorld.getServer().getSaveProperties());
            GeneratorOptions originalOpts = levelProperties.getGeneratorOptions();
            long seed = originalOpts.getSeed();
            MinecraftServerDataOwner accessedServer = ((MinecraftServerDataOwner) originalWorld.getServer());
            RegistryOps<Tag> nbtRegOps = RegistryOps.of(
                    NbtOps.INSTANCE,
                    accessedServer.seedcustomizer$getResourceManager(),
                    accessedServer.seedcustomizer$getRegistryTracker()
            );
            GeneratorOptions newOpts = GeneratorOptions.CODEC
                    .encodeStart(nbtRegOps, originalOpts)
                    .flatMap(tag ->
                            GeneratorOptions.CODEC.parse(
                                    this.recursivelySetSeed(new Dynamic<>(nbtRegOps, tag), seed, new ArrayList<>())
                            )
                    )
                    .get()
                    .map(
                            l -> l,
                            error -> {
                                throw new IllegalStateException("Unable to map GeneratorOptions: " + error.message());
                            }
                    );
            levelProperties.setGeneratorOptions(newOpts);
            DimensionType dimension = originalWorld.getDimension();
            RegistryKey<World> worldRegKey = originalWorld.getRegistryKey();
            RegistryKey<DimensionType> dimensionRegKey = accessedServer.seedcustomizer$getRegistryTracker().getDimensionTypeRegistry().getKey(dimension).orElseThrow(() -> new IllegalStateException("Unregistered dimension type: " + dimension));
            DimensionOptions dimGenOpts = newOpts.getDimensionMap().get(worldRegKey.getValue());
            Preconditions.checkNotNull(dimGenOpts, "No DimensionOptions for %s", worldRegKey);
            try (ServerWorld serverWorld = new ServerWorld(
                    originalWorld.getServer(),
                    Util.getServerWorkerExecutor(),
                    session,
                    ((ServerWorldProperties) originalWorld.getLevelProperties()),
                    worldRegKey,
                    dimensionRegKey,
                    dimension,
                    null,
                    dimGenOpts.getChunkGenerator(),
                    originalWorld.isDebugWorld(),
                    seed,
                    ImmutableList.of(),
                    false
            )) {
                this.regenForWorld(serverWorld);
                while (originalWorld.getServer().runTask()) {
                    Thread.yield();
                }
            } finally {
                levelProperties.setGeneratorOptions(originalOpts);
            }
        } finally {
            SafeFiles.tryHardToDeleteDir(tempDir);
        }
    }

    @Unique
    @SuppressWarnings("UnstableApiUsage")
    private void regenForWorld(ServerWorld serverWorld) {
        List<CompletableFuture<Chunk>> chunkLoadings = this.submitChunkLoadTasks(serverWorld);
        ((ServerChunkManagerAccessor) serverWorld.getChunkManager())
                .getMainThreadExecutor()
                .runTasks(() -> {
                    if (chunkLoadings.stream().anyMatch(ftr -> ftr.isDone() && Futures.getUnchecked(ftr) == null)) { return false; }
                    return chunkLoadings.stream().allMatch(CompletableFuture::isDone);
                });

        for (CompletableFuture<Chunk> future : chunkLoadings) {
            @Nullable Chunk chunk = future.getNow(null);
            Preconditions.checkState(chunk != null, "Failed to generate a chunk, regen failed.");
            ChunkPos cp = chunk.getPos();
            for (BlockPos blockPos : BlockPos.iterate(cp.getStartX(), 0, cp.getStartZ(), cp.getEndX(), chunk.getHeight(), cp.getEndZ())) {
                BlockState state = chunk.getBlockState(blockPos);
                BlockEntity blockEntity = chunk.getBlockEntity(blockPos);
                if (blockEntity != null) {
                    CompoundTag tag = new CompoundTag();
                    blockEntity.toTag(tag);
                    state = NbtHelper.toBlockState(tag);
                }
                chunk.setBlockState(blockPos, state, false);
                BiomeArray biomeArray = Preconditions.checkNotNull(chunk.getBiomeArray());
                Biome biome = biomeArray.getBiomeForNoiseGen(blockPos.getX(), blockPos.getY(), blockPos.getZ());
                ((OverridableBiomeArrayOwner) biomeArray).seedcustomizer$addOverride(blockPos.getX(), blockPos.getZ(), biome);
            }
        }
    }

    @Unique
    private List<CompletableFuture<Chunk>> submitChunkLoadTasks(ServerWorld world) {
        ServerChunkManagerAccessor chunkManager = (ServerChunkManagerAccessor) world.getChunkManager();
        ChunkTicketManagerAccessor ticketManager = ((ChunkTicketManagerAccessor) chunkManager.getTicketManager());
        List<CompletableFuture<Chunk>> chunkLoadings = new ArrayList<>();
        for (Long chunkPos : ticketManager.getChunkPositions()) {
            ChunkPos chunk = new ChunkPos(chunkPos);
            chunkLoadings.add(chunkManager
                    .callGetChunkFuture(chunk.x, chunk.z, ChunkStatus.FEATURES, true)
                    .thenApply(either -> either.left().orElse(null))
            );
        }
        return chunkLoadings;
    }

    @Unique
    @SuppressWarnings("unchecked")
    private Dynamic<Tag> recursivelySetSeed(Dynamic<Tag> dynamic, long seed, List<Dynamic<Tag>> seen) {
        if (seen.contains(dynamic)) { return dynamic; }
        seen.add(dynamic);
        return dynamic.updateMapValues(pair -> {
            if (pair.getFirst().asString("").equals("seed")) {
                return pair.mapSecond(v -> v.createLong(seed));
            }
            if (pair.getSecond().getValue() instanceof net.minecraft.nbt.CompoundTag) {
                return pair.mapSecond(v -> this.recursivelySetSeed((Dynamic<Tag>) v, seed, seen));
            }
            return pair;
        });
    }
}
