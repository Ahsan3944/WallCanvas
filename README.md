# WallCanvas

WallCanvas is a Minecraft 1.21.11 server-side custom artwork system with shared **Core** logic and native **Fabric** and **Paper** adapters.

## Architecture

```text
WallCanvas/
├── wallcanvas-core/   # Canonical shared models, image processing, persistence and pack generation
├── wallcanvas-fabric/ # Fabric 1.21.11 adapter
└── wallcanvas-paper/  # Paper 1.21.11 adapter
```

Painting and Map are separate display systems, while their domain definitions and processing live in the shared Core module.

## Pictures

The server picture library is:

```text
<world>/wallcanvas/pictures/
```

Supported formats currently include:

- PNG
- JPG / JPEG
- WebP

Pictures can be discovered dynamically through command tab completion. Web images can also be imported over HTTPS.

## WallCanvas Painting

WallCanvas Painting uses the native Minecraft **Painting** item/entity. **Item Frames are not used.**

Players receive a WallCanvas Painting item and place it normally on a wall. The artwork and selected canvas profile are stored with the Painting and are restored when the Painting is picked up/broken.

Canvas dimensions are measured directly in Minecraft blocks:

- `1 x 1` = 1 block wide × 1 block high
- `4 x 3` = 4 blocks wide × 3 blocks high
- Up to `16 x 16` blocks

Pixel density is independent of physical size and can be selected from `4` to `256` pixels per block using power-of-two values.

Built-in profiles include `2 x 2` and `4 x 2`. Custom profiles are persisted in:

```text
<world>/wallcanvas/painting-sizes.json
```

Generated Painting data/resource packs are rebuilt automatically. A server restart is required after introducing a new Painting variant so the variant can be registered before world initialization.

### Painting commands

```text
/wallcanvas list
/wallcanvas info <picture>
/wallcanvas create web <name> <url>
/wallcanvas give <player> <picture>
/wallcanvas give <player> <picture> <width> <height>
/wallcanvas give <player> <picture> <width> <height> <pixels-per-block>
```

The picture argument supports dynamic tab completion.

## WallCanvas Map

WallCanvas Map is a separate system based on normal Minecraft **Map** items and native `ItemDisplay` entities. It does not use Item Frames.

A map display is positioned by its **center coordinate**. If coordinates are omitted, the player's current position is used. Multi-tile displays can be created up to `64 x 64` map tiles.

```text
/wallcanvas map create <picture>
/wallcanvas map create <picture> <x> <y> <z>
/wallcanvas map create <picture> <x> <y> <z> <width> <height>
/wallcanvas map create web <name> <url>
/wallcanvas map give <picture>
/wallcanvas map remove <display-uuid>
```

Map artwork is generated from the same server picture library and persists through the shared display store.

## Resource Packs

Painting artwork requires the generated resource pack on clients. The generated archive is created as part of server initialization.

Configure an externally hosted HTTPS resource-pack URL in the platform configuration. WallCanvas calculates and sends the generated pack's SHA-1 hash to clients.

The generated pack is **not** automatically hosted by WallCanvas.

## Persistence

Display definitions are stored in:

```text
<world>/wallcanvas/displays.json
```

Painting size profiles are stored separately in:

```text
<world>/wallcanvas/painting-sizes.json
```

## Requirements

- Minecraft 1.21.11
- Java 21
- Fabric Loader 0.18.1 + Fabric API 0.141.6+1.21.11, or
- Paper 1.21.11

## Build

```bash
gradle build --no-daemon
```

GitHub Actions builds Core, Fabric and Paper together and publishes the resulting JARs as workflow artifacts.
