# AnarchyClient

Fabric `26.2` utility mod for 6b6t.

## Demo

Preview of how it looks in-game.

![Demo Preview](assets/demo.png)

## How to use 

1. Grab the latest Fabric .jar from https://github.com/6b6t/AnarchyClient/releases/latest
2. Put in your Fabric `minecraft/mods` folder.
3. Join a world
4. Open with Right-Shift click (keybind configurable in Minecraft keybind settings)

## Development

```bash
./gradlew build
./gradlew runClient
```

The client menu opens with `Right Shift`. Rivet is pulled from `com.github.Lenni0451.rivet:core:40c6f6c3a7`; upstream source is available at `https://github.com/Lenni0451/rivet`.
