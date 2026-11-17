package ru.beggar.berlimatrm.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

/** Dark checkbox with a cyan tick, matching the Berlit palette. */
public class AtRmCheckbox extends GuiButton {

    private static final int BOX = 10;
    private static final int FILL = 0xFF14171D;
    private static final int FILL_CHECKED = 0xFF1B2A2C;
    private static final int BORDER = 0xFF3A4150;
    private static final int BORDER_CHECKED = 0xFF2C8C8A;
    private static final int BORDER_HOVER = 0xFF6FE9E0;
    private static final int TEXT = 0xFFC8D2DE;
    private static final int TEXT_DISABLED = 0xFF8A94A6;

    private final String label;
    private boolean checked;

    public AtRmCheckbox(int id, int x, int y, String label, boolean checked) {
        super(id, x, y, 13 + Minecraft.getMinecraft().fontRenderer.getStringWidth(label), BOX, label);
        this.label = label;
        this.checked = checked;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public void toggle() {
        checked = !checked;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) {
            return;
        }

        boolean hovered = enabled && mouseX >= xPosition
            && mouseY >= yPosition
            && mouseX < xPosition + 13 + mc.fontRenderer.getStringWidth(label)
            && mouseY < yPosition + BOX;

        int border = !enabled ? BORDER : hovered ? BORDER_HOVER : checked ? BORDER_CHECKED : BORDER;
        int fill = checked ? FILL_CHECKED : FILL;

        drawRect(xPosition, yPosition, xPosition + BOX, yPosition + BOX, fill);
        drawRect(xPosition, yPosition, xPosition + BOX, yPosition + 1, border);
        drawRect(xPosition, yPosition + BOX - 1, xPosition + BOX, yPosition + BOX, border);
        drawRect(xPosition, yPosition, xPosition + 1, yPosition + BOX, border);
        drawRect(xPosition + BOX - 1, yPosition, xPosition + BOX, yPosition + BOX, border);

        if (checked) {
            int tick = enabled ? 0xFF6FE9E0 : 0xFF4E7C7A;
            drawRect(xPosition + 2, yPosition + 5, xPosition + 4, yPosition + 7, tick);
            drawRect(xPosition + 4, yPosition + 3, xPosition + 6, yPosition + 5, tick);
            drawRect(xPosition + 6, yPosition + 1, xPosition + 8, yPosition + 3, tick);
        }

        mc.fontRenderer.drawString(label, xPosition + 13, yPosition + 1, enabled ? TEXT : TEXT_DISABLED);
    }
}
