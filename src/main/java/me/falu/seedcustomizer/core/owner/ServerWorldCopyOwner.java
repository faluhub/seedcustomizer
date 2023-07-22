package me.falu.seedcustomizer.core.owner;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.level.storage.LevelStorage;

import java.io.IOException;

public interface ServerWorldCopyOwner {
    ServerWorld seedcustomizer$createCopy() throws IOException;
}
