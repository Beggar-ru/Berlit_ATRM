package ru.beggar.berlimatrm.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

/** One entry of the shaped / shapeless / remove switch. */
public class ModeButton extends GuiButton {

    private static final int FILL = 0xFF2B303C;
    private static final int FILL_HOVER = 0xFF3A4252;
    private static final int FILL_SELECTED = 0xFF2E3B44;
    private static final int BORDER = 0xFF3A4150;
    private static final int BORDER_HOVER = 0xFF6FE9E0;
    private static final int TEXT = 0xFFC8D2DE;
    private static final int TEXT_SELECTED = 0xFFE8F1F5;

    private boolean selected;

    public ModeButton(int id, int x, int y, int width, int height, String label) {
        super(id, x, y, width, height, label);
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) {
            return;
        }

        boolean hovered = enabled && mouseX >= xPosition
            && mouseY >= yPosition
            && mouseX < xPosition + width
            && mouseY < yPosition + height;

        int fill = selected ? FILL_SELECTED : hovered ? FILL_HOVER : FILL;
        int border = selected ? GuiTheme.ACCENT : hovered ? BORDER_HOVER : BORDER;

        drawRect(xPosition, yPosition, xPosition + width, yPosition + height, fill);
        drawRect(xPosition, yPosition, xPosition + width, yPosition + 1, border);
        drawRect(xPosition, yPosition + height - 1, xPosition + width, yPosition + height, border);
        drawRect(xPosition, yPosition, xPosition + 1, yPosition + height, border);
        drawRect(xPosition + width - 1, yPosition, xPosition + width, yPosition + height, border);

        if (selected) {
            drawRect(xPosition + 2, yPosition + 2, xPosition + 4, yPosition + height - 2, GuiTheme.ACCENT);
        }

        drawString(
            mc.fontRenderer,
            displayString,
            xPosition + 8,
            yPosition + (height - 8) / 2 + 1,
            selected ? TEXT_SELECTED : TEXT);
    }
}
