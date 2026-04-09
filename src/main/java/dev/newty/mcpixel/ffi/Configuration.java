package dev.newty.mcpixel.ffi;

import org.incendo.cloud.parser.flag.FlagContext;

public record Configuration(Integer size, Boolean stretch, Integer colours, Float brightness, Float saturation,
                            Float smoothing, Boolean overlay) {
    public static Configuration fromFlags(FlagContext flags) {
        Integer size = flags.get("size");
        Boolean stretch = flags.hasFlag("stretch");
        Integer colours = flags.get("colours");
        Float brightness = flags.get("brightness");
        Float saturation = flags.get("saturation");
        Float smoothing = flags.get("smoothing");
        Boolean overlay = flags.hasFlag("overlay");

        return new Configuration(size, stretch, colours, brightness, saturation, smoothing, overlay);
    }
}
