package me.m0dii.srvcron.utils;

import me.m0dii.srvcron.SRVCron;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Scanner;
import java.util.function.Consumer;

public class UpdateChecker {
    private final SRVCron plugin;
    private final int resourceId;

    public UpdateChecker(SRVCron plugin, int resourceId) {
        this.plugin = plugin;
        this.resourceId = resourceId;
    }

    public void getVersion(final Consumer<String> consumer) {
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () ->
        {
            try (InputStream inputStream = URI.create("https://api.spigotmc.org/legacy/update.php?resource=" + this.resourceId).toURL().openStream();

                 Scanner scanner = new Scanner(inputStream)) {
                if (scanner.hasNext()) {
                    consumer.accept(scanner.next());
                }
            } catch (IOException ex) {
                plugin.log("Failed to check for updates: " + ex.getMessage());
            }
        });
    }
}
