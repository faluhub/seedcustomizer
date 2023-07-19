package me.falu.seedcustomizer.mixin.access;

import com.mojang.datafixers.util.Either;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ChunkTicketManager;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.concurrent.CompletableFuture;

@Mixin(ServerChunkManager.class)
public interface ServerChunkManagerAccessor {
    @Invoker("getChunkFuture") CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> callGetChunkFuture(int chunkX, int chunkZ, ChunkStatus leastStatus, boolean create);

    @Accessor("mainThreadExecutor") ServerChunkManager.MainThreadExecutor getMainThreadExecutor();
    @Accessor("ticketManager") ChunkTicketManager getTicketManager();
}
