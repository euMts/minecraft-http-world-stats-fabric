package dev.matheuspass.http_world_stats;

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
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Difficulty;

public class Http_world_stats implements ModInitializer {

    private final LocalHttpServer http = new LocalHttpServer();
    private static final int HTTP_PORT = 8080;

    @Override
    public void onInitialize() {
        // Start/stop with the Minecraft server lifecycle
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            http.start(HTTP_PORT);
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            http.stop();
        });

        // Update snapshot every tick (or every N ticks if you want)
        ServerTickEvents.END_SERVER_TICK.register(this::updateSnapshot);
    }

    private void updateSnapshot(MinecraftServer server) {
        JsonObject root = new JsonObject();

        String worldName = server.getSaveProperties().getLevelName();
        boolean hardcore = server.getSaveProperties().isHardcore();
        Difficulty difficulty = server.getSaveProperties().getDifficulty();

        // General server/world info
        root.addProperty("hardcore", hardcore);
        root.addProperty("difficulty", difficulty.getName()); // "peaceful|easy|normal|hard"
        root.addProperty("motd", server.getServerMotd());
        root.addProperty("tickTimeMsAvg", server.getAverageTickTime());
        root.addProperty("playerCount", server.getCurrentPlayerCount());
        root.addProperty("maxPlayers", server.getMaxPlayerCount());
        root.addProperty("worldName", worldName);

        // Pick overworld (you can also iterate all worlds)
        ServerWorld overworld = server.getWorld(World.OVERWORLD);

        if (overworld != null) {
            long timeOfDay = overworld.getTimeOfDay(); // ticks do "tempo do dia" (influencia /time)
            long dayCount = Math.floorDiv(timeOfDay, 24000L); // 0 = primeiro dia
            root.addProperty("dayTime", overworld.getTimeOfDay()); // day-time (ticks)
            root.addProperty("time", overworld.getTime()); // total world time
            root.addProperty("seed", overworld.getSeed());
            root.addProperty("dayCount", dayCount);

            // se você quiser começar em 1 em vez de 0:
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

        // loadedChunksCount por dimensão
        JsonArray dimensions = new JsonArray();

        for (ServerWorld world : server.getWorlds()) {
            JsonObject d = new JsonObject();

            Identifier dimId = world.getRegistryKey().getValue();
            d.addProperty("dimension", dimId.toString());

            // loaded chunk count (pode variar por versão/Yarn; tenta o caminho comum)
            int loadedChunks = -1;
            try {
                loadedChunks = world.getChunkManager().getLoadedChunkCount();
            } catch (Throwable ignored) {
                // se sua versão não tiver esse método, fica -1 até ajustarmos pro seu mapping
            }
            d.addProperty("loadedChunksCount", loadedChunks);

            dimensions.add(d);
        }

        root.add("dimensions", dimensions);

        http.setWorldSnapshot(root);
    }
}
