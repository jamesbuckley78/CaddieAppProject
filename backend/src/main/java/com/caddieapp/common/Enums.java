package com.caddieapp.common;

/** Shared enums. Stored in the database as their names (VARCHAR), so never rename a value. */
public final class Enums {

    private Enums() {
    }

    public enum Role { CADDIE, ADMIN }

    public enum Grade { SINGLE, DOUBLE, JUNIOR }

    /** A single is 1 bag, a double is 2 (docs/RULES.md). */
    public enum BagType {
        SINGLE(1), DOUBLE(2);

        private final int bags;

        BagType(int bags) {
            this.bags = bags;
        }

        public int bags() {
            return bags;
        }
    }

    /**
     * A caddie's status for today (queue_entries.status).
     * OFF = checked out / finished. FOOD and BACK_LATER keep their place in line.
     * CALLED and OUT are set by the admin when assigning bags (next feature).
     */
    public enum QueueStatus { OFF, AVAILABLE, FOOD, BACK_LATER, CALLED, OUT }

    /** Where a newly created caddie goes in the club list. */
    public enum Placement { TOP_OF_GROUP, BOTTOM_OF_GROUP }
}
