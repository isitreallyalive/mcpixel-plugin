package dev.newty.mcpixel.ffi;

import org.jetbrains.annotations.NotNull;

public record BlockPair(@NotNull String baseId, boolean baseTop, String overlayId, boolean overlayTop) {
}
