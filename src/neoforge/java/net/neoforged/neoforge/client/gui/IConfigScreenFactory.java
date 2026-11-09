package net.neoforged.neoforge.client.gui;

import net.minecraft.client.gui.screens.Screen;
import net.neoforged.fml.IExtensionPoint;
import net.neoforged.fml.ModContainer;

@FunctionalInterface
public interface IConfigScreenFactory extends IExtensionPoint {
    Screen createScreen(ModContainer container, Screen modListScreen);
}
