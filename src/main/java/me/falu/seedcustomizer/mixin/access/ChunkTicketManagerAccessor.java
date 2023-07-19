package me.falu.seedcustomizer.mixin.access;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.world.ChunkTicketManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkTicketManager.class)
public interface ChunkTicketManagerAccessor {
    @Accessor("chunkPositions") LongSet getChunkPositions();
}
