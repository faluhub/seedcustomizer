package me.falu.seedcustomizer.core;

import it.unimi.dsi.fastutil.longs.LongSet;
import me.falu.seedcustomizer.core.owner.ServerWorldCopyOwner;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;

import java.io.IOException;

public class ChunkRegenerator {
    public static final ChunkRegenerator INSTANCE = new ChunkRegenerator();

    public void regenerate(ServerWorld world, ServerPlayerEntity player) {
        player.sendMessage(new LiteralText("Started Regeneration Process...").formatted(Formatting.GRAY, Formatting.ITALIC), true);
        long start = Util.getMeasuringTimeMs();
        try (ServerWorld copy = ((ServerWorldCopyOwner) world).seedcustomizer$createCopy()) {
            LongSet chunkPositions = world.getChunkManager().threadedAnvilChunkStorage.loadedChunks;
            int size = chunkPositions.size();
            int index = 0;
            for (Long chunkPosLong : chunkPositions) {
                index++;
                ChunkPos chunkPos = new ChunkPos(chunkPosLong);
                Chunk chunk = copy.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FEATURES, true);
                if (chunk != null) {
                    Iterable<BlockPos> blocks = BlockPos.iterate(
                            chunk.getPos().getStartX(),
                            0,
                            chunk.getPos().getStartZ(),
                            chunk.getPos().getEndX(),
                            world.getChunkManager().getChunkGenerator().getMaxY(),
                            chunk.getPos().getEndZ()
                    );
                    for (BlockPos blockPos : blocks) {
                        BlockState state = chunk.getBlockState(blockPos);
                        world.setBlockState(blockPos, state);
                    }
                }
                int percentage = Math.round((float) index / size * 100.0F);
                player.sendMessage(new LiteralText("Finished Chunk " + index + " of " + size + " (" + percentage + "%)").formatted(Formatting.GRAY, Formatting.ITALIC), true);
            }
            long diff = Util.getMeasuringTimeMs() - start;
            player.sendMessage(new LiteralText("Completed Regeneration Process. (" + (diff / 1000) + " Seconds)").formatted(Formatting.GREEN, Formatting.BOLD), true);
        } catch (IOException e) {
            player.sendMessage(new LiteralText("Failed Regeneration Process.").formatted(Formatting.DARK_RED, Formatting.BOLD), true);
            e.printStackTrace();
        }
    }
}
