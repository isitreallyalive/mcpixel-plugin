package dev.newty.mcpixel;

import dev.newty.mcpixel.ffi.Configuration;
import dev.newty.mcpixel.ffi.McPixel;
import dev.newty.mcpixel.ffi.Texture;
import dev.newty.mcpixel.parsers.UrlParser;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.bukkit.parser.location.LocationParser;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.parser.flag.CommandFlag;
import org.incendo.cloud.parser.flag.FlagContext;
import org.incendo.cloud.parser.standard.FloatParser;
import org.incendo.cloud.parser.standard.IntegerParser;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
                // configuration
                .flag(flag(INT, "size", "s"))
                .flag(flag("stretch"))
                .flag(flag(INT, "colours", "c"))
                .flag(flag(FLOAT, "brightness", "b"))
                .flag(flag(FLOAT, "saturation"))
                .flag(flag(FLOAT, "smooth"))
                .flag(flag("overlay"))
                // mc specific
                .flag(flag(LocationParser.locationParser(), "origin", "o"))
                .handler(PixelCommand::handle)
                .build();
    }

    private static void handle(@NonNull CommandContext<Player> ctx) {
        // fetch image
        URL url = ctx.get("url");
        byte[] image;

        try (InputStream is = url.openStream()) {
            image = is.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // create art
        FlagContext flags = ctx.flags();
        Configuration config = Configuration.fromFlags(flags);
        long art = McPixel.newArt(image, config);
        int[] dimensions = McPixel.artDimensions(art);
        List<Texture> textures = Arrays.asList(McPixel.artBlocks(art));
        McPixel.freeArt(art);

        Collections.reverse(textures);

        // build art
        Player player = ctx.sender();
        World world = player.getWorld();
        int width = dimensions[0];
        int height = dimensions[1];

        // determine origin
        Location origin = flags.get("origin");
        if (origin == null) origin = player.getLocation();
        origin.setYaw(Math.round(origin.getYaw() / 90.0f) * 90.0f); // snap to nearest cardinal

        // vectors
        Vector forward = origin.getDirection().normalize();
        Vector up = new Vector(0, 1, 0);
        Vector right = up.clone().crossProduct(forward).normalize();

        Vector startOffset = forward.clone().multiply(2).subtract(right.clone().multiply(width / 2));
        origin.add(startOffset);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // find the relevant texture
                int idx = y * width + x;
                Texture texture = textures.get(idx);
                if (texture == null) continue;

                // find the associated world coordinates
                Vector offset = right.clone().multiply(x).add(up.clone().multiply(y));
                Location blockLocation = origin.clone().add(offset);
                blockLocation.subtract(0, 1, 0);

                // find the associated block
                Material material = Material.matchMaterial(String.format("minecraft:%s", texture.baseId()));
                if (material == null) continue;

                world.getBlockAt(blockLocation).setType(material, false);
            }
        }
    }
}
