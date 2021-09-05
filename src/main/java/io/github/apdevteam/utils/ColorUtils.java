package io.github.apdevteam.utils;

import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class ColorUtils {
    // Discord Colors
    /**
     * #5865F2
     * CMYK: 56, 43, 0, 0
     * RGB: 88, 101, 242
     * HSV: 236, 64, 95
     */
    private static final @NotNull Color DISCORD_BLURPLE = new Color(0xFF5865F2, true);
    /**
     * #57F287
     * CMYK: 50, 0, 55, 0
     * RGB: 87, 242, 135
     * HSV: 140, 64, 95
     */
    private static final @NotNull Color DISCORD_GREEN = new Color(0xFF57F287, true);
    /**
     * #FEE75C
     * CMYK: 0, 5, 80, 0
     * RGB: 254, 231, 92
     * HSV: 52, 64, 100
     */
    private static final @NotNull Color DISCORD_YELLOW = new Color(0xFFFEE75C, true);
    /**
     * #EB459E
     * CMYK: 0, 90, 0, 0
     * RGB: 235, 69, 158
     * HSV: 328, 71, 92
     */
    private static final @NotNull Color DISCORD_FUCHSIA = new Color(0xFFEB459E, true);
    /**
     * #ED4245
     * CMYK: 0, 90, 65, 0
     * RGB: 237, 66, 69
     * HSV: 0, 72, 93
     */
    private static final @NotNull Color DISCORD_RED = new Color(0xFFED4245, true);
    /**
     * #FFFFFF
     * CMYK: 0, 0, 0, 0
     * RGB: 255, 255, 255
     * HSV: *, 0, 100
     */
    private static final @NotNull Color DISCORD_WHITE = new Color(0xFFFFFFFF, true);
    /**
     * #000000
     * CMYK: 35, 0, 0, 100
     * RGB: 0, 0, 0
     * HSV: *, 0, 0
     */
    private static final @NotNull Color DISCORD_BLACK = new Color(0xFF000000, true);

    // Discord like colors
    /**
     * #33F2FF
     * CMYK: 80, 5, 0, 0
     * RGB: 51, 242, 255
     * HSV: 184, 80, 100
     */
    private static final @NotNull Color DISCORD_ISH_CYAN = new Color(0xFF46ECFB, true);

    // Brighter colors
    /**
     * #FF4040
     * CMYK: 0, 75, 75, 0
     * RGB: 255, 64, 64
     * HSV: 0, 75, 100
     */
    private static final @NotNull Color RED = new Color(0xFFFF4040, true);

    // Bright colors
    /**
     * #FF0000
     * CMYK: 0, 100, 100, 0
     * RGB: 255, 0, 0
     * HSV: 0, 100, 100
     */
    private static final @NotNull Color BRIGHT_RED = new Color(0xFFFF0000, true);
    /**
     * #00FF00
     * CMYK: 100, 0, 100, 0
     * RGB: 0, 255, 0
     * HSV: 120, 100, 100
     */
    private static final @NotNull Color BRIGHT_GREEN = new Color(0xFF00FF00, true);
    /**
     * #0000FF
     * CMYK: 100, 100, 0, 0
     * RGB: 240, 0, 255
     * HSV: 240, 100, 100
     */
    private static final @NotNull Color BRIGHT_BLUE = new Color(0xFF0000FF, true);
    /**
     * #FFFF00
     * CMYK: 0, 0, 100, 0
     * RGB: 255, 255, 0
     * HSV: 60, 100, 100
     */
    private static final @NotNull Color BRIGHT_YELLOW = new Color(0xFFFFFF00, true);


    // Success
    public static @NotNull Color blocked() {
        return DISCORD_GREEN;
    }

    public static @NotNull Color unblocked() {
        return DISCORD_GREEN;
    }

    // Misc
    public static @NotNull Color initialMessage() {
        return DISCORD_GREEN;
    }

    public static @NotNull Color forwardFromUser() {
        return DISCORD_YELLOW;
    }

    public static @NotNull Color forwardToUser() {
        return DISCORD_GREEN;
    }

    public static @NotNull Color invite() {
        return DISCORD_FUCHSIA;
    }

    public static @NotNull Color userBlocked() {
        return DISCORD_FUCHSIA;
    }

    public static @NotNull Color beginModMail() {
        return DISCORD_ISH_CYAN;
    }

    public static @NotNull Color closeModMail() {
        return DISCORD_RED;
    }

    // Errors
    public static @NotNull Color invalidCmd() {
        return DISCORD_RED;
    }

    public static @NotNull Color addError() {
        return DISCORD_RED;
    }

    public static @NotNull Color notInbox() {
        return DISCORD_RED;
    }

    public static @NotNull Color blockFailed() {
        return DISCORD_RED;
    }

    public static @NotNull Color unblockFailed() {
        return DISCORD_RED;
    }

    public static @NotNull Color closeFailed() {
        return DISCORD_RED;
    }

    public static @NotNull Color existingModMail() {
        return DISCORD_RED;
    }

    public static @NotNull Color noExistingModMail() {
        return DISCORD_RED;
    }

    public static @NotNull Color invalidUser() {
        return DISCORD_RED;
    }

    // Internal Colors
    public static @NotNull Color startup() {
        return BRIGHT_GREEN;
    }

    public static @NotNull Color shutdown() {
        return BRIGHT_RED;
    }

    public static @NotNull Color log() {
        return BRIGHT_BLUE;
    }

    public static @NotNull Color debug() {
        return BRIGHT_YELLOW;
    }

    public static @NotNull Color error() {
        return RED;
    }
}
