package ru.beggar.berlimatrm.gui;

import net.minecraft.client.gui.GuiTextField;

import org.lwjgl.opengl.GL11;

/**
 * GuiTextField with the Berlit frame instead of the vanilla grey box. Background drawing is
 * disabled and our own recess is painted behind the text, so caret/selection handling stays
 * vanilla.
 */
public class ThemedTextField extends GuiTextField {

    private static final int TEXT = 0xFFC8D2DE;
    private static final int TEXT_DISABLED = 0xFF6B7382;

    private boolean enabled = true;

    public ThemedTextField(net.minecraft.client.gui.FontRenderer fontRenderer, int x, int y, int width, int height) {
        super(fontRenderer, x, y, width, height);
        setEnableBackgroundDrawing(false);
        setMaxStringLength(512);
        setCanLoseFocus(true);
        setTextColor(TEXT);
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        this.enabled = enabled;
        if (!enabled) {
            setFocused(false);
        }
    }

    @Override
    public void drawTextBox() {
        int border = !enabled ? 0xFF2A2F38 : isFocused() ? GuiTheme.ACCENT : 0xFF3A4150;
        drawRect(xPosition - 1, yPosition - 1, xPosition + width + 1, yPosition + height + 1, border);
        drawRect(xPosition, yPosition, xPosition + width, yPosition + height, GuiTheme.WELL);

        if (!getVisible()) {
            return;
        }

        setTextColor(enabled ? TEXT : TEXT_DISABLED);
        GL11.glTranslatef(2.0F, 2.0F, 0.0F);
        super.drawTextBox();
        GL11.glTranslatef(-2.0F, -2.0F, 0.0F);
    }

    @Override
    public boolean textboxKeyTyped(char letter, int key) {
        return enabled && super.textboxKeyTyped(letter, key);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int button) {
        if (!enabled) {
            return;
        }
        super.mouseClicked(mouseX, mouseY, button);
    }
}
