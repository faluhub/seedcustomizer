package me.falu.seedcustomizer.mixin;

import com.google.common.collect.ImmutableList;
import me.falu.seedcustomizer.core.owner.ServerWorldCopyOwner;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Util;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.Spawner;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.level.ServerWorldProperties;
import net.minecraft.world.level.storage.LevelStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin extends World implements ServerWorldCopyOwner {
    @Shadow @NotNull public abstract MinecraftServer getServer();
    @Shadow @Final private ServerWorldProperties worldProperties;
    @Shadow public abstract ServerChunkManager getChunkManager();
    @Shadow public abstract long getSeed();
    @Unique Executor workerExecutor;

    protected ServerWorldMixin(MutableWorldProperties mutableWorldProperties, RegistryKey<World> registryKey, RegistryKey<DimensionType> registryKey2, DimensionType dimensionType, Supplier<Profiler> profiler, boolean bl, boolean bl2, long l) {
        super(mutableWorldProperties, registryKey, registryKey2, dimensionType, profiler, bl, bl2, l);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void extraVariables(MinecraftServer server, Executor workerExecutor, LevelStorage.Session session, ServerWorldProperties properties, RegistryKey<World> registryKey, RegistryKey<DimensionType> registryKey2, DimensionType dimensionType, WorldGenerationProgressListener generationProgressListener, ChunkGenerator chunkGenerator, boolean bl, long l, List<Spawner> list, boolean bl2, CallbackInfo ci) {
        this.workerExecutor = workerExecutor;
    }

    @Override
    public ServerWorld seedcustomizer$createCopy() throws IOException {
        Path tempDir = Files.createTempDirectory("ChunkRegen");
        LevelStorage levelStorage = LevelStorage.create(tempDir);
        try (LevelStorage.Session session = levelStorage.createSession("ChunkRegenTemp")) {
            return new ServerWorld(
                    this.getServer(),
                    Util.getServerWorkerExecutor(),
                    session,
                    this.worldProperties,
                    this.getRegistryKey(),
                    this.getDimensionRegistryKey(),
                    this.getDimension(),
                    new WorldGenerationProgressListener() {
                        @Override public void start(ChunkPos spawnPos) {}
                        @Override public void setChunkStatus(ChunkPos pos, @Nullable ChunkStatus status) {}
                        @Override public void stop() {}
                    },
                    this.getChunkManager().getChunkGenerator(),
                    this.isDebugWorld(),
                    this.getSeed(),
                    ImmutableList.of(),
                    false
            );
        }
    }
}
