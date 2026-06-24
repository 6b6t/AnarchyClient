---
layout: default
---

AnarchyClient is a Fabric utility mod built for 6b6t on Minecraft `26.2`. It ships
a custom in-game GUI on a tailored Rivet interface, renders through Blaze3D on both
Vulkan and OpenGL, and bundles combat, movement, and awareness modules behind a
single Right-Shift menu.

![AnarchyClient running in-game](assets/demo.png)

## Features

- Custom, unique in-game GUI for fast and consistent module control, with selectable animated panel backgrounds.
- Vulkan and OpenGL rendering through Blaze3D.
- Combat and survival: `KillAura`, `AutoTotem`, `AutoArmor`, `AutoWeapon`, `AutoSprint`.
- Movement: `Parkour`, `NoFall`, `SafeWalk`, `Eagle`, `AutoEat`.
- Awareness: `ESP`, `StorageEsp`, `ItemEsp`, `BlockEsp`, `Tracers`, `Nametags`, `Trajectories`.
- HUD: `CoordinatesHud`, `ActiveModulesHud`.

## Install

1. Download the latest Fabric jar from the [releases page](https://github.com/6b6t/AnarchyClient/releases/latest).
2. Drop it into your `minecraft/mods` folder.
3. Join a world.
4. Press Right-Shift to open the menu. The keybind is configurable in Minecraft's controls.

## Verify your download

Release jars are signed with build provenance, so you can confirm a jar was built
by this repo's GitHub Actions before you run it:

```bash
gh attestation verify anarchyclient-mc-26.2-VERSION.jar --repo 6b6t/AnarchyClient
```

## Build from source

```bash
./gradlew build
./gradlew runClient
```

Source and contribution details live on [GitHub](https://github.com/6b6t/AnarchyClient).
