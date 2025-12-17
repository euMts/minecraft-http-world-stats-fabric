# HTTP World Stats

A Minecraft Fabric mod that exposes world statistics via a local HTTP server.

## Requirements

This mod requires:
- **Minecraft Version:** 1.21.11
- **Fabric Loader:** You need to install [Fabric](https://fabricmc.net) to run this mod in Minecraft

Visit [https://fabricmc.net](https://fabricmc.net) to download and install Fabric Loader for Minecraft 1.21.11.

## Building

To build the mod, run:
```bash
.\gradlew.bat build
```

**Note:** Make sure to set `org.gradle.java.home` in `gradle.properties` to point to your Java installation.

The compiled mod JAR files will be located in:
```
build\libs
```

## HTTP Server

The mod starts an embedded HTTP server when the Minecraft server starts. The server is accessible at:

- **IP:** `127.0.0.1` (localhost only)
- **Port:** `8080`

### Endpoints

- **`/health`** - Health check endpoint (returns "ok")
- **`/world`** - Returns JSON with world statistics including:
  - `hardcore` - Whether the world is in hardcore mode (boolean)
  - `difficulty` - World difficulty ("peaceful", "easy", "normal", or "hard")
  - `motd` - Server message of the day
  - `tickTimeMsAvg` - Average tick time in milliseconds
  - `playerCount` - Current number of players online
  - `maxPlayers` - Maximum number of players allowed
  - `worldName` - Name of the world/save
  - `dayTime` - Current time of day in ticks (0-24000)
  - `time` - Total world time in ticks
  - `seed` - World seed
  - `dayCount` - Number of days elapsed (0-based)
  - `dayNumberHuman` - Human-readable day number (1-based)
  - `dimension` - Overworld dimension identifier
  - `players` - Array of all online players, each containing:
    - `name` - Player name
    - `uuid` - Player UUID
    - `x`, `y`, `z` - Player coordinates
  - `dimensions` - Array of all loaded dimensions, each containing:
    - `dimension` - Dimension identifier
    - `loadedChunksCount` - Number of loaded chunks in that dimension

#### Example Response

```json
{
  "hardcore": false,
  "difficulty": "normal",
  "motd": "A Minecraft Server",
  "tickTimeMsAvg": 12.5,
  "playerCount": 2,
  "maxPlayers": 20,
  "worldName": "My World",
  "dayTime": 6000,
  "time": 24000,
  "seed": 1234567890,
  "dayCount": 1,
  "dayNumberHuman": 2,
  "dimension": "minecraft:overworld",
  "players": [
    {
      "name": "Steve",
      "uuid": "8667b71b-86b0-4bfa-9693-32707ddcc527",
      "x": 100.5,
      "y": 64.0,
      "z": 200.3
    },
    {
      "name": "Alex",
      "uuid": "61699b2e-d327-4a01-9f1e-296a35585225",
      "x": -50.2,
      "y": 70.0,
      "z": 150.8
    }
  ],
  "dimensions": [
    {
      "dimension": "minecraft:overworld",
      "loadedChunksCount": 441
    },
    {
      "dimension": "minecraft:the_nether",
      "loadedChunksCount": 196
    },
    {
      "dimension": "minecraft:the_end",
      "loadedChunksCount": 25
    }
  ]
}
```

### Example Usage

Once the mod is loaded and a world is running, you can access:

- World stats: `http://127.0.0.1:8080/world`

The world snapshot is updated every server tick, so the data is always current.