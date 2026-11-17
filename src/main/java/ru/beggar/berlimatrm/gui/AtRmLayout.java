package ru.beggar.berlimatrm.gui;

/** Geometry of the ATRM screen, shared by the container and the GUI so they cannot drift apart. */
public final class AtRmLayout {

    public static final int WIDTH = 340;
    public static final int CONTENT_HEIGHT = 276;

    public static final int INVENTORY_WIDTH = 176;
    public static final int INVENTORY_HEIGHT = 98;
    public static final int INVENTORY_X = (WIDTH - INVENTORY_WIDTH) / 2;
    public static final int INVENTORY_TOP = CONTENT_HEIGHT + 8;
    public static final int HEIGHT = INVENTORY_TOP + INVENTORY_HEIGHT;

    public static final int SLOT = 18;
    public static final int GRID = 9;
    public static final int GRID_PIXELS = GRID * SLOT;
    public static final int GRID_X = 8;
    public static final int GRID_Y = 40;

    public static final int MENU_X = GRID_X + GRID_PIXELS + 8;
    public static final int MENU_Y = GRID_Y;
    public static final int MENU_WIDTH = WIDTH - MENU_X - 8;
    public static final int MENU_HEIGHT = GRID_PIXELS;

    public static final int SELECT_X = 8;
    public static final int SELECT_Y = 18;
    public static final int SELECT_HEIGHT = 18;

    public static final int OPTIONS_X = 8;
    public static final int OPTIONS_Y = 206;
    public static final int OPTIONS_WIDTH = WIDTH - 16;
    public static final int OPTIONS_HEIGHT = 66;
    /** Inner padding of the options well. */
    public static final int OPTIONS_INNER_X = OPTIONS_X + 6;

    /** Result slot (item position, the frame is drawn one pixel to the left/up). */
    public static final int RESULT_X = MENU_X + 68;
    public static final int RESULT_Y = MENU_Y + 82;

    public static final int[] MODE_Y = { MENU_Y + 14, MENU_Y + 32, MENU_Y + 50 };
    public static final int MODE_HEIGHT = 16;

    public static final int MIRROR_X = MENU_X + 6;
    public static final int MIRROR_Y = MENU_Y + 70;

    public static final int CLEAR_X = MENU_X + 6;
    public static final int CLEAR_Y = MENU_Y + 104;
    public static final int CLEAR_WIDTH = MENU_WIDTH - 12;
    public static final int CLEAR_HEIGHT = 18;

    public static final int OK_X = MENU_X + 6;
    public static final int OK_Y = MENU_Y + 124;
    public static final int OK_WIDTH = MENU_WIDTH - 12;
    public static final int OK_HEIGHT = 18;

    public static final int CLOSE_X = MENU_X + 6;
    public static final int CLOSE_Y = MENU_Y + 144;
    public static final int CLOSE_WIDTH = MENU_WIDTH - 12;
    public static final int CLOSE_HEIGHT = 18;

    public static final int ORE_ROW_Y = OPTIONS_Y + 30;
    public static final int ORE_LABEL_X = OPTIONS_INNER_X;
    public static final int ORE_PREV_X = OPTIONS_INNER_X + 26;
    public static final int ORE_NAME_X = OPTIONS_INNER_X + 44;
    public static final int ORE_NAME_WIDTH = 126;
    public static final int ORE_NEXT_X = OPTIONS_INNER_X + 174;
    public static final int ORE_BUTTON_SIZE = 14;
    public static final int RESET_X = OPTIONS_INNER_X + 194;
    public static final int RESET_WIDTH = 124;

    public static final int NBT_ROW_Y = OPTIONS_Y + 47;
    public static final int NBT_LABEL_X = OPTIONS_INNER_X;
    public static final int NBT_FIELD_X = OPTIONS_INNER_X + 28;
    public static final int NBT_FIELD_WIDTH = 200;
    public static final int NBT_FIELD_HEIGHT = 12;
    public static final int FROM_ITEM_X = OPTIONS_INNER_X + 234;
    public static final int FROM_ITEM_WIDTH = 84;

    public static final int CHECKBOX_Y = OPTIONS_Y + 16;
    public static final int CHECKBOX_HEIGHT = 10;

    public static final int PLAYER_X = INVENTORY_X;
    public static final int PLAYER_TOP = INVENTORY_TOP;
    public static final int PLAYER_SLOT_X = PLAYER_X + 8;
    public static final int PLAYER_SLOT_Y = PLAYER_TOP + 18;
    public static final int PLAYER_HOTBAR_Y = PLAYER_TOP + 74;

    public static int gridX(int col) {
        return GRID_X + col * SLOT;
    }

    public static int gridY(int row) {
        return GRID_Y + row * SLOT;
    }

    public static int cellIndex(int col, int row) {
        return col + row * GRID;
    }

    private AtRmLayout() {}
}
