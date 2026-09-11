package com.ultraop.wallcanvas.core.map;

/** Shared world-space placement math for map tiles on both platform adapters. */
public final class MapPlacement {
    private MapPlacement() {
    }

    public static double x(double centerX, float yaw, int tileX, int tilesWide) {
        return centerX + rightX(yaw) * horizontalOffset(tileX, tilesWide);
    }

    public static double z(double centerZ, float yaw, int tileX, int tilesWide) {
        return centerZ + rightZ(yaw) * horizontalOffset(tileX, tilesWide);
    }

    public static double y(double centerY, int tileY, int tilesHigh) {
        return centerY + (tilesHigh - 1) / 2.0 - tileY;
    }

    private static double horizontalOffset(int tileX, int tilesWide) {
        return tileX - (tilesWide - 1) / 2.0;
    }

    /** Minecraft yaw 0 faces south; the screen's right side therefore points west. */
    private static double rightX(float yaw) {
        return -Math.cos(Math.toRadians(yaw));
    }

    private static double rightZ(float yaw) {
        return -Math.sin(Math.toRadians(yaw));
    }
}
