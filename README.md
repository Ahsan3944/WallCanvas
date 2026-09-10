# WallCanvas

Server-side custom wall art and map displays for Minecraft 1.21.11.

WallCanvas is designed to support both **Fabric** and **Paper** through a shared core architecture.

## Planned Features

- Custom wall-mounted image paintings
- PNG, JPG, JPEG and WebP image support
- Automatic image discovery from a server-side image library
- Public image URL import
- Placeable custom painting items
- Unlimited placement/duplication of saved artwork
- Precise center-coordinate positioning
- Move, resize and rotate placed artwork
- In-game image selection and placement workflow
- Custom Minecraft map displays
- Map placement by a chosen center coordinate
- Map/screenshot artwork workflows without requiring players to physically reach the target area
- Server-side caching and persistent display data
- Fabric Mod support
- Paper Plugin support
- Minecraft 1.21.11 target

## Project Structure

```text
WallCanvas/
├── wallcanvas-core/   # Shared domain, storage and display logic
├── wallcanvas-fabric/ # Fabric 1.21.11 integration
└── wallcanvas-paper/  # Paper 1.21.11 integration
```

## Status

Early development. The repository is intentionally starting from a clean foundation.
