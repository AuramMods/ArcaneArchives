package com.aranaira.arcanearchives.arsenal;

import java.util.Locale;

public enum GemType {
    SLAUGHTERGLEAM(Cut.ASSCHER, Color.RED, 30, 150, true),
    MURDERGLEAM(Cut.ASSCHER, Color.YELLOW, 30, 150, true),
    AGEGLEAM(Cut.ASSCHER, Color.GREEN, 30, 150, false),
    CLEANSEGLEAM(Cut.ASSCHER, Color.BLUE, 30, 150, false),
    SWITCHGLEAM(Cut.ASSCHER, Color.PURPLE, 30, 150, false),
    SALVEGLEAM(Cut.ASSCHER, Color.PINK, 30, 150, true),
    MUNCHSTONE(Cut.OVAL, Color.BLACK, 60, 240, false),
    ORDERSTONE(Cut.OVAL, Color.PINK, 100, 400, false),
    MINDSPINDLE(Cut.PAMPEL, Color.GREEN, 800, 3600, false),
    ELIXIRSPINDLE(Cut.PAMPEL, Color.PURPLE, 5, 20, false),
    MOUNTAINTEAR(Cut.PENDELOQUE, Color.ORANGE, 25, 100, false),
    RIVERTEAR(Cut.PENDELOQUE, Color.BLUE, 25, 100, false),
    PARCHTEAR(Cut.PENDELOQUE, Color.BLACK, 8000, 27000, false),
    PHOENIXWAY(Cut.TRILLION, Color.ORANGE, 75, 300, false),
    STORMWAY(Cut.TRILLION, Color.YELLOW, 30, 150, true);

    public final Cut cut;
    public final Color color;
    public final int capacity;
    public final int upgradedCapacity;
    public final boolean toggle;

    GemType(Cut cut, Color color, int capacity, int upgradedCapacity, boolean toggle) {
        this.cut = cut;
        this.color = color;
        this.capacity = capacity;
        this.upgradedCapacity = upgradedCapacity;
        this.toggle = toggle;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public enum Cut {
        ASSCHER, OVAL, PAMPEL, PENDELOQUE, TRILLION
    }

    public enum Color {
        NONE(0xFFFFFF), RED(0xFF8080), ORANGE(0xFFBF80), YELLOW(0xFFFF80), GREEN(0x80FF99),
        CYAN(0x80FFFF), BLUE(0x80A6FF), PURPLE(0xCC80FF), PINK(0xFF8CFF), BLACK(0x666666), WHITE(0xFFFFFF);

        public final int rgb;

        Color(int rgb) {
            this.rgb = rgb;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        static Color fromOrdinal(int ordinal) {
            return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : NONE;
        }
    }
}
