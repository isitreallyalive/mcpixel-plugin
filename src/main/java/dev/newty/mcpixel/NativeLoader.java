package dev.newty.mcpixel;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class NativeLoader {
    private static volatile boolean loaded = false;

    private static String getTarget() {
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();

        if (os.contains("win") && arch.contains("64"))
            return "x86_64-pc-windows-msvc";
        else if (os.contains("linux") && arch.contains("aarch64"))
            return "aarch64-unknown-linux-gnu";
        else if (os.contains("linux") && arch.contains("64"))
            return "x86_64-unknown-linux-gnu";
        else if (os.contains("mac") && arch.contains("aarch64"))
            return "aarch64-apple-darwin";
        else if (os.contains("mac") && arch.contains("x86_64"))
            return "x86_64-apple-darwin";
        else
            throw new UnsupportedOperationException(STR."Unsupported OS/arch: \{os}/\{arch}");
    }

    public static synchronized void load() {
        if (loaded) return;

        String target = getTarget();
        String resourcePath = "/natives/" + target;

        try (InputStream in = NativeLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null)
                throw new RuntimeException("Native library not found in jar: " + resourcePath);

            Path temp = Files.createTempFile("ffi-", null);
            temp.toFile().deleteOnExit();

            Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
            System.load(temp.toAbsolutePath().toString());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load native library", e);
        }

        loaded = true;
    }
}
