package dev.matheuspass.http_world_stats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.Difficulty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Http_world_stats implements ModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("http_world_stats");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private final LocalHttpServer http = new LocalHttpServer();
    private static final int HTTP_PORT = 8080;

    @Override
    public void onInitialize() {
        // Start/stop with the Minecraft server lifecycle
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            http.start(HTTP_PORT);
            logWorldInfo(server);
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            http.stop();
        });

        // Update snapshot every tick (or every N ticks if you want)
        ServerTickEvents.END_SERVER_TICK.register(this::updateSnapshot);
    }

    private void logWorldInfo(MinecraftServer server) {
        JsonObject worldInfo = collectWorldInfo(server);
        LOGGER.info("=== World Loaded ===");
        LOGGER.info("World Statistics:\n{}", GSON.toJson(worldInfo));
        LOGGER.info("===================");
    }

    private JsonObject collectWorldInfo(MinecraftServer server) {
        JsonObject root = new JsonObject();

        String worldName = server.getSaveProperties().getLevelName();
        boolean hardcore = server.getSaveProperties().isHardcore();
        Difficulty difficulty = server.getSaveProperties().getDifficulty();

        // General server/world info
        root.addProperty("hardcore", hardcore);
        root.addProperty("difficulty", difficulty.getName());
        root.addProperty("motd", server.getServerMotd());
        root.addProperty("tickTimeMsAvg", server.getAverageTickTime());
        root.addProperty("playerCount", server.getCurrentPlayerCount());
        root.addProperty("maxPlayers", server.getMaxPlayerCount());
        root.addProperty("worldName", worldName);

        // Pick overworld
        ServerWorld overworld = server.getWorld(World.OVERWORLD);

        if (overworld != null) {
            long timeOfDay = overworld.getTimeOfDay();
            long dayCount = Math.floorDiv(timeOfDay, 24000L);
            root.addProperty("dayTime", overworld.getTimeOfDay());
            root.addProperty("time", overworld.getTime());
            root.addProperty("seed", overworld.getSeed());
            root.addProperty("dayCount", dayCount);
            root.addProperty("dayNumberHuman", dayCount + 1);

            RegistryKey<World> key = overworld.getRegistryKey();
            Identifier dimId = key.getValue();
            root.addProperty("dimension", dimId.toString());
        }

        // Players
        JsonArray players = new JsonArray();
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            JsonObject pj = new JsonObject();
            pj.addProperty("name", p.getName().getString());
            pj.addProperty("uuid", p.getUuidAsString());
            pj.addProperty("x", p.getX());
            pj.addProperty("y", p.getY());
            pj.addProperty("z", p.getZ());
            players.add(pj);
        }

        root.add("players", players);

        // Dimensions with loaded chunks
        JsonArray dimensions = new JsonArray();

        for (ServerWorld world : server.getWorlds()) {
            JsonObject d = new JsonObject();

            Identifier dimId = world.getRegistryKey().getValue();
            d.addProperty("dimension", dimId.toString());

            int loadedChunks = -1;
            try {
                loadedChunks = world.getChunkManager().getLoadedChunkCount();
            } catch (Throwable ignored) {
                // if version doesn't have this method, stays -1
            }
            d.addProperty("loadedChunksCount", loadedChunks);

            dimensions.add(d);
        }

        root.add("dimensions", dimensions);

        return root;
    }

    private void updateSnapshot(MinecraftServer server) {
        JsonObject root = collectWorldInfo(server);
        http.setWorldSnapshot(root);
    }
}
