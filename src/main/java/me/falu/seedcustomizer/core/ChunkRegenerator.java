package me.falu.seedcustomizer.core;

import me.falu.seedcustomizer.core.owner.ServerWorldCopyOwner;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class ChunkRegenerator {
    public static final ChunkRegenerator INSTANCE = new ChunkRegenerator();

    public void regenerate(ServerWorld world, ServerPlayerEntity player) {
        try (ServerWorld copy = ((ServerWorldCopyOwner) world).seedcustomizer$createCopy()) {
            Chunk chunk = copy.getChunk(player.chunkX, player.chunkZ, ChunkStatus.FEATURES);
            Iterable<BlockPos> blocks = BlockPos.iterate(
                    chunk.getPos().getStartX(),
                    0,
                    chunk.getPos().getStartZ(),
                    chunk.getPos().getEndX(),
                    world.getChunkManager().getChunkGenerator().getMaxY(),
                    chunk.getPos().getEndZ()
            );
            for (BlockPos pos : blocks) {
                world.setBlockState(pos, chunk.getBlockState(pos));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
