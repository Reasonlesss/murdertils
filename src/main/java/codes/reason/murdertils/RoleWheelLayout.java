package codes.reason.murdertils;

record RoleWheelBounds(int x, int y, int width, int height) {
    boolean contains(double x, double y) {
        return x >= this.x && x <= this.x + width
                && y >= this.y && y <= this.y + height;
    }
}

final class RoleWheelLayout {
    static final int TILE_WIDTH = 84;
    static final int TILE_HEIGHT = 52;
    static final double INNER_RADIUS = 34.0;
    static final double OUTER_RADIUS = 168.0;

    private static final double START_ANGLE = -Math.PI / 2.0;
    private static final double FULL_TURN = Math.PI * 2.0;

    private RoleWheelLayout() {
    }

    static RoleWheelBounds boundsFor(Role role, int centerX, int centerY) {
        double angle = angleFor(role);
        double distance = maximumCenterDistanceInsideCircle(angle) - 1.0;
        int tileCenterX = (int) Math.round(centerX + Math.cos(angle) * distance);
        int tileCenterY = (int) Math.round(centerY + Math.sin(angle) * distance);
        return new RoleWheelBounds(
                tileCenterX - TILE_WIDTH / 2,
                tileCenterY - TILE_HEIGHT / 2,
                TILE_WIDTH,
                TILE_HEIGHT
        );
    }

    static Role roleAt(double cursorX, double cursorY, double centerX, double centerY) {
        for (Role role : Role.values()) {
            if (boundsFor(role, (int) centerX, (int) centerY).contains(cursorX, cursorY)) {
                return role;
            }
        }

        double dx = cursorX - centerX;
        double dy = cursorY - centerY;
        double radius = Math.hypot(dx, dy);
        if (radius < INNER_RADIUS || radius > OUTER_RADIUS) {
            return null;
        }

        Role[] roles = Role.values();
        double sectorSize = FULL_TURN / roles.length;
        double clockwiseFromTop = normalizeAngle(Math.atan2(dy, dx) - START_ANGLE);
        int index = (int) Math.floor((clockwiseFromTop + sectorSize / 2.0) / sectorSize) % roles.length;
        return roles[index];
    }

    static double angleFor(Role role) {
        return START_ANGLE + role.ordinal() * FULL_TURN / Role.values().length;
    }

    private static double maximumCenterDistanceInsideCircle(double angle) {
        double halfWidth = TILE_WIDTH / 2.0;
        double halfHeight = TILE_HEIGHT / 2.0;
        double outwardProjection = Math.abs(Math.cos(angle)) * halfWidth
                + Math.abs(Math.sin(angle)) * halfHeight;
        double cornerRadiusSquared = halfWidth * halfWidth + halfHeight * halfHeight;

        // Solves the quadratic for the center distance where the outermost
        // rectangle corner is exactly on the wheel's circular boundary.
        return -outwardProjection + Math.sqrt(
                outwardProjection * outwardProjection
                        + OUTER_RADIUS * OUTER_RADIUS
                        - cornerRadiusSquared
        );
    }

    private static double normalizeAngle(double angle) {
        double normalized = angle % FULL_TURN;
        return normalized < 0.0 ? normalized + FULL_TURN : normalized;
    }
}
