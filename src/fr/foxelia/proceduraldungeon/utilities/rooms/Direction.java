package fr.foxelia.proceduraldungeon.utilities.rooms;

/**
 * Represents a cardinal direction for room entrances and exits.
 * Direction is determined by which face of the selection the point is on.
 */
public enum Direction {

    NORTH(0),   // -Z face
    EAST(90),   // +X face
    SOUTH(180), // +Z face
    WEST(270);  // -X face

    private final int rotation;

    Direction(int rotation) {
        this.rotation = rotation;
    }

    /**
     * Get the rotation angle in degrees (clockwise from north).
     */
    public int getRotation() {
        return rotation;
    }

    /**
     * Get the opposite direction.
     */
    public Direction opposite() {
        return switch (this) {
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case EAST -> WEST;
            case WEST -> EAST;
        };
    }

    /**
     * Calculate how many degrees clockwise to rotate from this direction to the target direction.
     * Used when connecting rooms: the exit direction of the previous room must align
     * with the entrance direction of the next room (facing each other).
     *
     * @param target The target direction to rotate towards
     * @return Rotation in degrees (0, 90, 180, or 270)
     */
    public int rotationTo(Direction target) {
        int diff = (target.rotation - this.rotation + 360) % 360;
        return diff;
    }

    /**
     * Get the direction after applying a rotation (in degrees clockwise).
     */
    public Direction rotate(int degrees) {
        int newRotation = (this.rotation + degrees + 360) % 360;
        for (Direction d : values()) {
            if (d.rotation == newRotation) return d;
        }
        return this;
    }

    /**
     * Parse direction from string (case-insensitive).
     */
    public static Direction fromString(String str) {
        if (str == null) return NORTH;
        return switch (str.toUpperCase()) {
            case "NORTH", "N" -> NORTH;
            case "EAST", "E" -> EAST;
            case "SOUTH", "S" -> SOUTH;
            case "WEST", "W" -> WEST;
            default -> NORTH;
        };
    }
}
