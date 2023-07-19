package me.falu.seedcustomizer.core;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.stream.Stream;

/**
 * Source: <a href="https://github.com/EngineHub/WorldEdit/blob/archive/1.16.5/worldedit-core/src/main/java/com/sk89q/worldedit/util/io/file/SafeFiles.java">WorldEdit</a>
 */
public class SafeFiles {
    /**
     * Recursively uses {@link #tryHardToDelete(Path)} to clean up directories before deleting them.
     *
     * @param directory the directory to delete
     * @throws IOException if an error occurs trying to delete the directory
     */
    public static void tryHardToDeleteDir(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            if (!Files.exists(directory)) {
                return;
            }

            throw new IOException(directory + " is not a directory");
        }
        try (Stream<Path> files = Files.list(directory)) {
            for (Iterator<Path> iter = files.iterator(); iter.hasNext();) {
                Path next = iter.next();
                if (Files.isDirectory(next)) {
                    tryHardToDeleteDir(next);
                } else {
                    tryHardToDelete(next);
                }
            }
        }
        tryHardToDelete(directory);
    }

    /**
     * Tries to delete a path. If it fails the first time, uses an implementation detail to try
     * and make it possible to delete the path, and then tries again. If that fails, throws an
     * {@link IOException} with both errors.
     *
     * @param path the path to delete
     * @throws IOException if the path could not be deleted after multiple attempts
     */
    public static void tryHardToDelete(Path path) throws IOException {
        IOException suppressed = tryDelete(path);
        if (suppressed == null) {
            return;
        }

        // This is copied from Ant (see org.apache.tools.ant.util.FileUtils.tryHardToDelete).
        // It mentions that there is a bug in the Windows JDK implementations that this is a valid
        // workaround for. I've been unable to find a definitive reference to this bug.
        // The thinking is that if this is good enough for Ant, it's good enough for us.
        System.gc();
        try {
            Thread.sleep(10);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        IOException suppressed2 = tryDelete(path);
        if (suppressed2 == null) {
            return;
        }
        IOException ex = new IOException("Failed to delete " + path, suppressed2);
        ex.addSuppressed(suppressed);
        throw ex;
    }

    @Nullable
    private static IOException tryDelete(Path path) {
        try {
            Files.deleteIfExists(path);
            if (Files.exists(path)) {
                return new IOException(path + " still exists after deleting");
            }
            return null;
        } catch (IOException e) {
            return e;
        }
    }
}
