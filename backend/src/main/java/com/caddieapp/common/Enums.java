package com.caddieapp.common;

/** Shared enums. Stored in the database as their names (VARCHAR), so never rename a value. */
public final class Enums {

    private Enums() {
    }

    public enum Role { CADDIE, ADMIN }

    public enum Grade { SINGLE, DOUBLE, JUNIOR }

    public enum BagType { SINGLE, DOUBLE }

    /** OFF -> AVAILABLE -> CALLED -> OUT -> (Back in) AVAILABLE, plus BREAK. */
    public enum QueueStatus { OFF, AVAILABLE, CALLED, OUT, BREAK }
}
