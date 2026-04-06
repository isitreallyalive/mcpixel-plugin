package dev.newty.mcpixel.ffi;

import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class NativeLoader {
    private static volatile boolean loaded;

    public static synchronized void load() {
        if (loaded) return;

        // find the correct library in the jar
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();
        String libraryPath = getLibraryPath(os, arch);

        try (InputStream in = NativeLoader.class.getResourceAsStream(libraryPath)) {
            if (in == null) throw new RuntimeException("Native library not found: " + libraryPath);

            // create a temporary file
            String suffix = os.contains("win") ? ".dll" : os.contains("mac") ? ".dylib" : ".so";
            Path temp = Files.createTempFile("ffi-", suffix);
            temp.toFile().deleteOnExit();

            // write it to the fs and load it
            Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
            System.load(temp.toAbsolutePath().toString());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load native library", e);
        }

        loaded = true;
    }

    private static @NonNull String getLibraryPath(String os, String arch) {
        String target;
        if (os.contains("win") && arch.contains("64")) target = "x86_64-pc-windows-msvc";
        else if (os.contains("linux") && arch.contains("aarch64")) target = "aarch64-unknown-linux-gnu";
        else if (os.contains("linux") && arch.contains("64")) target = "x86_64-unknown-linux-gnu";
        else if (os.contains("mac") && arch.contains("aarch64")) target = "aarch64-apple-darwin";
        else if (os.contains("mac") && arch.contains("x86_64")) target = "x86_64-apple-darwin";
        else throw new UnsupportedOperationException(os + "/" + arch);

        return String.format("/natives/%s", target);
    }
}