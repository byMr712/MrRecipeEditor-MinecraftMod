package com.recipeeditor.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;

import java.util.List;

/**
 * Adapter bridging legacy DrawContext calls to Minecraft 1.21.11 GuiGraphics.
 */
public class DrawContext {
    private final GuiGraphics graphics;

    public DrawContext(GuiGraphics graphics) {
        this.graphics = graphics;
    }

    public GuiGraphics getGraphics() {
        return graphics;
    }

    public Matrix3x2fStack getMatrices() {
        return graphics.pose();
    }

    public void fill(int minX, int minY, int maxX, int maxY, int color) {
        graphics.fill(minX, minY, maxX, maxY, color);
    }

    public void fillGradient(int minX, int minY, int maxX, int maxY, int colorStart, int colorEnd) {
        graphics.fillGradient(minX, minY, maxX, maxY, colorStart, colorEnd);
    }

    public void drawItem(ItemStack stack, int x, int y) {
        graphics.renderItem(stack, x, y);
    }

    public void drawItem(ItemStack stack, int x, int y, int seed) {
        graphics.renderItem(stack, x, y, seed);
    }

    public void drawStackOverlay(Font font, ItemStack stack, int x, int y) {
        graphics.renderItemDecorations(font, stack, x, y);
    }

    public void drawStackOverlay(Font font, ItemStack stack, int x, int y, String text) {
        graphics.renderItemDecorations(font, stack, x, y, text);
    }

    public void drawTextWithShadow(Font font, Component text, int x, int y, int color) {
        graphics.drawString(font, text, x, y, color, true);
    }

    public void drawTextWithShadow(Font font, FormattedCharSequence text, int x, int y, int color) {
        graphics.drawString(font, text, x, y, color, true);
    }

    public void drawTextWithShadow(Font font, String text, int x, int y, int color) {
        graphics.drawString(font, text, x, y, color, true);
    }

    public void drawCenteredTextWithShadow(Font font, Component text, int centerX, int y, int color) {
        graphics.drawCenteredString(font, text, centerX, y, color);
    }

    public void drawCenteredTextWithShadow(Font font, String text, int centerX, int y, int color) {
        graphics.drawCenteredString(font, text, centerX, y, color);
    }

    public void drawItemTooltip(Font font, ItemStack stack, int x, int y) {
        graphics.setTooltipForNextFrame(font, stack, x, y);
    }

    public void drawTooltip(Font font, Component text, int x, int y) {
        graphics.setTooltipForNextFrame(text, x, y);
    }

    public void drawTooltip(Font font, List<Component> text, int x, int y) {
        graphics.setComponentTooltipForNextFrame(font, text, x, y);
    }

    public void enableScissor(int minX, int minY, int maxX, int maxY) {
        graphics.enableScissor(minX, minY, maxX, maxY);
    }

    public void disableScissor() {
        graphics.disableScissor();
    }
}
