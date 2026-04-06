package dev.newty.mcpixel;

import dev.newty.mcpixel.ffi.Texture;
import dev.newty.mcpixel.ffi.McPixel;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;

public class PixelCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NonNull CommandSender sender, org.bukkit.command.@NonNull Command command, @NonNull String label, String @NonNull [] args) {
        // make sure there are args
        if (args.length < 4) return false;

        // make sure the command was sent by a player
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Must be a player");
            return true;
        }

        // parse coordinates
        int x, y, z;

        try {
            x = Integer.parseInt(args[args.length - 3]);
            y = Integer.parseInt(args[args.length - 2]);
            z = Integer.parseInt(args[args.length - 1]);
        } catch (NumberFormatException e) {
            sender.sendMessage("Invalid coordinates");
            return true;
        }

        // parse URL
        URL url;

        try {
            String urlString = String.join(" ", Arrays.copyOf(args, args.length - 3));
            url = URI.create(urlString).toURL();
        } catch (MalformedURLException e) {
            sender.sendMessage("Invalid URL");
            return true;
        }

        // read the image data at the URL
        byte[] data;

        try {
            data = url.openStream().readAllBytes();
        } catch (IOException e) {
            sender.sendMessage("Failed to read image data: " + e.getMessage());
            return true;
        }

        // generate the pixel art
        long art = McPixel.newArt(data);
        Texture[] blocks = McPixel.artBlocks(art);
        int[] dimensions = McPixel.artDimensions(art);
        int width = dimensions[0];
        int height = dimensions[1];

        // build the pixel art
        World world = player.getWorld();
        Location origin = new Location(world, x, y, z);

        for (int iy = 0; iy < height; iy++) {
            for (int ix = 0; ix < width; ix++) {
                // find the relevant texture
                int idx = iy * width + ix;
                Texture pair = blocks[idx];
                if (pair == null) continue;

                // find the associated world coordinates
                int worldX = origin.getBlockX() + width - ix - 1;
                int worldY = origin.getBlockY() + height - iy - 1;
                int worldZ = origin.getBlockZ();

                // find the associated block
                Material material = Material.matchMaterial(String.format("minecraft:%s", pair.baseId()));
                if (material == null) continue;

                world.getBlockAt(worldX, worldY, worldZ).setType(material);
            }
        }

        return true;
    }
}
