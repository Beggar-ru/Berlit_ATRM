package ru.beggar.berlimatrm.gui;

/**
 * Colours of the Berlit interface (slate panel, cyan frame).
 *
 * <p>
 * Kept identical to the Berlit mod palette so the two mods look like one family. The values also
 * exist in the 9-slice kit texture; change them here only together with the kit.
 */
public final class GuiTheme {

    /** Body text on the dark panel. */
    public static final int TEXT = 0xFFC8D2DE;

    /** Screen title, slightly brighter than the body. */
    public static final int TITLE = 0xFFE8F1F5;

    /** Cyan accent, matches the frame highlight in the texture. */
    public static final int ACCENT = 0xFF6FE9E0;

    /** Dimmed text for disabled widgets. */
    public static final int TEXT_DISABLED = 0xFF6B7382;

    /** Panel gradient. */
    public static final int PANEL_TOP = 0xFF363B48;

    public static final int PANEL_BOTTOM = 0xFF262A34;

    /** Frame highlight. */
    public static final int FRAME_LIGHT = 0xFF6FE9E0;

    /** Title rule under the header, dimmer than the frame. */
    public static final int FRAME_BORDER = 0xFF2C8C8A;

    public static final int FRAME_RULE = 0xFF386868;

    /** Recessed strip / text field background. */
    public static final int WELL = 0xFF12151B;

    public static final int WELL_BORDER = 0xFF2C8C8A;

    /** Button plate and its border. */
    public static final int BUTTON_FILL = 0xFF323A48;

    public static final int BUTTON_BORDER = 0xFF2C8C8A;

    /** Positive / negative status colours. */
    public static final int OK = 0xFF7BE38A;

    public static final int ERROR = 0xFFFF7B7B;

    /** Dim highlight for the hovered star button. */
    public static final int STAR = 0xFF5E8B8A;

    public static final int STAR_HOVER = 0xFFB8FFF8;

    private GuiTheme() {}
}
