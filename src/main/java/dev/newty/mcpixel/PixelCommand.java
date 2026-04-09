package dev.newty.mcpixel;

import dev.newty.mcpixel.ffi.Configuration;
import dev.newty.mcpixel.ffi.McPixel;
import dev.newty.mcpixel.ffi.Texture;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.bukkit.parser.location.LocationParser;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.parser.flag.CommandFlag;
import org.incendo.cloud.parser.flag.FlagContext;
import org.incendo.cloud.parser.standard.BooleanParser;
import org.incendo.cloud.parser.standard.FloatParser;
import org.incendo.cloud.parser.standard.IntegerParser;

import java.io.IOException;
import java.net.URL;

public final class PixelCommand {
    private static final ParserDescriptor<Object, Integer> INT = IntegerParser.integerParser();
    private static final ParserDescriptor<Object, Float> FLOAT = FloatParser.floatParser();

    private static <T> CommandFlag<T> flag(
            ParserDescriptor<? super Object, T> parser,
            String longName,
            String... aliases
    ) {
        return CommandFlag.<T>builder(longName)
                .withAliases(aliases)
                .withComponent(parser)
                .build();
    }

    private static CommandFlag<Void> flag(String longName, String... aliases) {
        return CommandFlag.<Void>builder(longName)
                .withAliases(aliases)
                .build();
    }

    public static Command<Player> build(LegacyPaperCommandManager<CommandSender> manager) {
        return manager.commandBuilder("pixel")
                .senderType(Player.class)
                // <url>
                .argument(UrlParser.component().name("url"))
                // <x> <y> <z>
                .argument(LocationParser.locationComponent().name("origin").optional())
                // flags
                .flag(flag(INT, "size", "s"))
                .flag(flag("stretch"))
                .flag(flag(INT, "colours", "c"))
                .flag(flag(FLOAT, "brightness", "b"))
                .flag(flag(FLOAT, "saturation"))
                .flag(flag(FLOAT, "smooth"))
                .flag(flag("overlay", "o"))
                .handler(PixelCommand::handle)
                .build();
    }

    private static void handle(@NonNull CommandContext<Player> ctx) {
        // fetch image
        URL url = ctx.get("url");
        byte[] image;

        try {
            image = url.openStream().readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // create art
        Configuration config = Configuration.fromFlags(ctx.flags());
        long art = McPixel.newArt(image, config);
        int[] dimensions = McPixel.artDimensions(art);
        Texture[] textures = McPixel.artBlocks(art);
        McPixel.freeArt(art);

        // build art
        Player player = ctx.sender();
        Location origin = ctx.getOrDefault("origin", player.getLocation());
        World world = player.getWorld();
        int width = dimensions[0];
        int height = dimensions[1];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // find the relevant texture
                int idx = y * width + x;
                Texture texture = textures[idx];
                if (texture == null) continue;

                // find the associated world coordinates
                int worldX = origin.getBlockX() + width - x - 2;
                int worldY = origin.getBlockY() + width - y - 2;
                int worldZ = origin.getBlockZ();

                // find the associated block
                Material material = Material.matchMaterial(String.format("minecraft:%s", texture.baseId()));
                if (material == null) continue;

                world.getBlockAt(worldX, worldY, worldZ).setType(material, false);
            }
        }
    }
}
