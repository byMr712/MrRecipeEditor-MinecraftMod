import os
import re
import subprocess
from pathlib import Path

def get_minecraft_version():
    props = Path("gradle.properties").read_text(encoding="utf-8")
    m = re.search(r"minecraft_version=([0-9\.]+)", props)
    if not m:
        raise ValueError("Could not find minecraft_version in gradle.properties")
    return m.group(1)

def is_gametest_supported(mc_ver):
    if mc_ver.startswith("26."):
        return True
    parts = [int(p) for p in mc_ver.split(".")]
    if len(parts) >= 3 and parts[0] == 1 and parts[1] == 21:
        return parts[2] >= 4
    return False

def is_identifier_version(mc_ver):
    if mc_ver.startswith("26."):
        return True
    parts = [int(p) for p in mc_ver.split(".")]
    if len(parts) >= 3 and parts[0] == 1 and parts[1] == 21:
        return parts[2] >= 11
    return False

def uses_mouse_events(mc_ver):
    if mc_ver.startswith("26."):
        return True
    parts = [int(p) for p in mc_ver.split(".")]
    if len(parts) >= 3 and parts[0] == 1 and parts[1] == 21:
        return parts[2] >= 9
    return False

def uses_posestack(mc_ver):
    if mc_ver.startswith("26."):
        return False
    parts = [int(p) for p in mc_ver.split(".")]
    if len(parts) >= 3 and parts[0] == 1 and parts[1] == 21:
        return parts[2] <= 5
    if len(parts) == 2 and parts[0] == 1 and parts[1] == 21:
        return True
    return False


def uses_ingredient_items(mc_ver):
    if mc_ver.startswith("26."):
        return False
    parts = [int(p) for p in mc_ver.split(".")]
    if len(parts) >= 3 and parts[0] == 1 and parts[1] == 21:
        return parts[2] <= 3
    if len(parts) == 2 and parts[0] == 1 and parts[1] == 21:
        return True
    return False

def adapt_classic_gui():
    print("Adapting GUI for classic mouse/keyboard events (<= 1.21.8)...")
    
    # 1. SelectableTextFieldWidget.java
    p_tf = Path("src/main/java/com/recipeeditor/client/gui/SelectableTextFieldWidget.java")
    t_tf = p_tf.read_text(encoding="utf-8")
    t_tf = t_tf.replace("import net.minecraft.client.input.MouseButtonEvent;\n", "")
    t_tf = re.sub(
        r"@Override\s+public boolean mouseClicked\(MouseButtonEvent event, boolean doubleClick\) \{.*?\n    \}",
        """@Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean result = super.mouseClicked(mouseX, mouseY, button);
        if (result && isLeftClick(button)) {
            long now = System.currentTimeMillis();
            if (now - lastClickTime < 300L && Math.abs(mouseX - lastClickX) < 5.0 && Math.abs(mouseY - lastClickY) < 5.0) {
                this.setCursorToStart(false);
                this.setSelectionEnd(this.getValue().length());
                this.dragAnchor = 0;
                this.isDraggingSelection = false;
                this.lastClickTime = 0L;
                return true;
            }
            this.lastClickTime = now;
            this.lastClickX = mouseX;
            this.lastClickY = mouseY;
            this.dragAnchor = this.getCursorPosition();
            this.isDraggingSelection = true;
        } else if (!this.isFocused()) {
            this.dragAnchor = -1;
            this.isDraggingSelection = false;
        }
        return result;
    }""",
        t_tf, flags=re.DOTALL
    )
    t_tf = re.sub(
        r"@Override\s+public boolean mouseDragged\(MouseButtonEvent event, double deltaX, double deltaY\) \{.*?\n    \}",
        """@Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (handleMouseDragged(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }""",
        t_tf, flags=re.DOTALL
    )
    t_tf = re.sub(
        r"@Override\s+public boolean mouseReleased\(MouseButtonEvent event\) \{.*?\n    \}",
        """@Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isLeftClick(button)) {
            this.isDraggingSelection = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }""",
        t_tf, flags=re.DOTALL
    )
    p_tf.write_text(t_tf, encoding="utf-8")

    # 2. RecipeConflictScreen.java
    p_cs = Path("src/main/java/com/recipeeditor/client/gui/RecipeConflictScreen.java")
    t_cs = p_cs.read_text(encoding="utf-8")
    t_cs = t_cs.replace("import net.minecraft.client.input.MouseButtonEvent;\n", "")
    t_cs = re.sub(
        r"@Override\s+public boolean mouseClicked\(MouseButtonEvent event, boolean doubleClick\) \{.*?\n    \}",
        """@Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double sX = mouseX / uiScale;
        double sY = mouseY / uiScale;

        if (super.mouseClicked(sX, sY, button)) {
            return true;
        }

        if (isLeftClick(button) && maxScroll > 0) {
            int vWidth = getVirtualWidth();
            int vHeight = getVirtualHeight();
            int paneWidth = Math.min(420, vWidth - 40);
            int paneX = (vWidth - paneWidth) / 2;
            int listTop = 40;
            int btnY = vHeight - 28;
            int listBottom = btnY - 6;
            int listH = Math.max(20, listBottom - listTop);
            int scrollbarX = paneX + paneWidth - 6;

            if (sX >= scrollbarX - 2 && sX <= scrollbarX + 6 && sY >= listTop && sY <= listBottom) {
                isDraggingScroll = true;
                dragScrollStartY = sY;
                dragScrollStartOffset = scrollOffset;
                return true;
            }
        }
        return false;
    }""",
        t_cs, flags=re.DOTALL
    )
    t_cs = re.sub(
        r"@Override\s+public boolean mouseReleased\(MouseButtonEvent event\) \{.*?\n    \}",
        """@Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        double sX = mouseX / uiScale;
        double sY = mouseY / uiScale;
        isDraggingScroll = false;
        return super.mouseReleased(sX, sY, button);
    }""",
        t_cs, flags=re.DOTALL
    )
    t_cs = re.sub(
        r"@Override\s+public boolean mouseDragged\(MouseButtonEvent event, double deltaX, double deltaY\) \{.*?\n    \}",
        """@Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        double sX = mouseX / uiScale;
        double sY = mouseY / uiScale;

        if (isDraggingScroll && isLeftClick(button) && maxScroll > 0) {
            int vHeight = getVirtualHeight();
            int listTop = 40;
            int btnY = vHeight - 28;
            int listBottom = btnY - 6;
            int listH = Math.max(20, listBottom - listTop);
            int totalContentHeight = maxScroll + listH - 8;
            int scrollbarH = Math.max(16, (int) ((float) listH / (float) totalContentHeight * listH));
            int trackH = listH - scrollbarH;
            if (trackH > 0) {
                double delta = sY - dragScrollStartY;
                scrollOffset = Math.max(0, Math.min(maxScroll, dragScrollStartOffset + (int) (delta / trackH * maxScroll)));
            }
            return true;
        }
        return super.mouseDragged(sX, sY, button, deltaX / uiScale, deltaY / uiScale);
    }""",
        t_cs, flags=re.DOTALL
    )
    p_cs.write_text(t_cs, encoding="utf-8")

    # 3. RecipeEditorScreen.java
    p_es = Path("src/main/java/com/recipeeditor/client/gui/RecipeEditorScreen.java")
    t_es = p_es.read_text(encoding="utf-8")
    t_es = t_es.replace("import net.minecraft.client.input.KeyEvent;\n", "")
    t_es = t_es.replace("import net.minecraft.client.input.MouseButtonEvent;\n", "")
    t_es = t_es.replace("this.minecraft.getWindow().handle()", "this.minecraft.getWindow().getWindow()")
    t_es = t_es.replace(
        "new net.minecraft.server.players.NameAndId(this.minecraft.player.getGameProfile())",
        "this.minecraft.player.getGameProfile()"
    )

    # mouseClicked
    t_es = re.sub(
        r"@Override\s+public boolean mouseClicked\(MouseButtonEvent event, boolean doubleClick\) \{",
        "@Override\n    public boolean mouseClicked(double mouseX, double mouseY, int button) {",
        t_es
    )
    t_es = t_es.replace("double sX = event.x() / uiScale;", "double sX = mouseX / uiScale;")
    t_es = t_es.replace("double sY = event.y() / uiScale;", "double sY = mouseY / uiScale;")
    t_es = t_es.replace("MouseButtonEvent scaledEvent = new MouseButtonEvent(sX, sY, event.buttonInfo());\n", "")
    t_es = t_es.replace("exitBtn.mouseClicked(scaledEvent, doubleClick)", "exitBtn.mouseClicked(sX, sY, button)")
    t_es = t_es.replace("toggleEnabledBtn.mouseClicked(scaledEvent, doubleClick)", "toggleEnabledBtn.mouseClicked(sX, sY, button)")
    t_es = t_es.replace("clearSearchBtn.mouseClicked(scaledEvent, doubleClick)", "clearSearchBtn.mouseClicked(sX, sY, button)")
    t_es = t_es.replace("super.mouseClicked(scaledEvent, doubleClick)", "super.mouseClicked(sX, sY, button)")
    t_es = t_es.replace("int button = event.button();\n", "")
    t_es = t_es.replace("event.hasShiftDown()", "Screen.hasShiftDown()")

    # mouseReleased
    t_es = re.sub(
        r"@Override\s+public boolean mouseReleased\(MouseButtonEvent event\) \{",
        "@Override\n    public boolean mouseReleased(double mouseX, double mouseY, int button) {",
        t_es
    )
    t_es = t_es.replace("return super.mouseReleased(new MouseButtonEvent(sX, sY, event.buttonInfo()));", "return super.mouseReleased(sX, sY, button);")
    t_es = t_es.replace("return super.mouseReleased(scaledEvent);", "return super.mouseReleased(sX, sY, button);")

    # mouseDragged
    t_es = re.sub(
        r"@Override\s+public boolean mouseDragged\(MouseButtonEvent event, double deltaX, double deltaY\) \{",
        "@Override\n    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {",
        t_es
    )
    t_es = t_es.replace("return super.mouseDragged(new MouseButtonEvent(sX, sY, event.buttonInfo()), deltaX / uiScale, deltaY / uiScale);", "return super.mouseDragged(sX, sY, button, deltaX / uiScale, deltaY / uiScale);")
    t_es = t_es.replace("return super.mouseDragged(scaledEvent, deltaX / uiScale, deltaY / uiScale);", "return super.mouseDragged(sX, sY, button, deltaX / uiScale, deltaY / uiScale);")

    # keyPressed
    t_es = re.sub(
        r"@Override\s+public boolean keyPressed\(KeyEvent event\) \{",
        "@Override\n    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {",
        t_es
    )
    t_es = t_es.replace("int keyCode = event.key();\n", "")
    t_es = t_es.replace("return super.keyPressed(event);", "return super.keyPressed(keyCode, scanCode, modifiers);")

    p_es.write_text(t_es, encoding="utf-8")
    print("Classic GUI adapted.")

def adapt_posestack_and_recipe_book():
    print("Adapting DrawContext (PoseStack) and ServerRecipeBook (<= 1.21.5)...")
    
    # DrawContext.java with PoseStack and renderTooltip
    draw_context_src = """package com.recipeeditor.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class DrawContext {
    private final GuiGraphics graphics;

    public static class MatricesAdapter {
        private final PoseStack pose;
        public MatricesAdapter(PoseStack pose) {
            this.pose = pose;
        }
        public void pushMatrix() {
            pose.pushPose();
        }
        public void scale(float x, float y) {
            pose.scale(x, y, 1.0f);
        }
        public void translate(float x, float y) {
            pose.translate(x, y, 0.0f);
        }
        public void popMatrix() {
            pose.popPose();
        }
    }

    public DrawContext(GuiGraphics graphics) {
        this.graphics = graphics;
    }

    public GuiGraphics getGraphics() {
        return graphics;
    }

    public MatricesAdapter getMatrices() {
        return new MatricesAdapter(graphics.pose());
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
        graphics.renderTooltip(font, stack, x, y);
    }

    public void drawTooltip(Font font, Component text, int x, int y) {
        graphics.renderTooltip(font, text, x, y);
    }

    public void drawTooltip(Font font, List<Component> text, int x, int y) {
        graphics.renderComponentTooltip(font, text, x, y);
    }

    public void enableScissor(int minX, int minY, int maxX, int maxY) {
        graphics.enableScissor(minX, minY, maxX, maxY);
    }

    public void disableScissor() {
        graphics.disableScissor();
    }
}
"""
    Path("src/main/java/com/recipeeditor/client/gui/DrawContext.java").write_text(draw_context_src, encoding="utf-8")

    # ServerRecipeBookMixin.java for 1.21.5
    srb_src = """package com.recipeeditor.mixin;

import com.recipeeditor.recipe.CustomRecipeDispatcher;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerRecipeBook;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerRecipeBook.class)
public class ServerRecipeBookMixin {

    @Inject(method = "contains", at = @At("HEAD"), cancellable = true)
    private void onContains(ResourceKey<Recipe<?>> key, CallbackInfoReturnable<Boolean> cir) {
        if (com.recipeeditor.RecipeEditorMod.isDedicatedServer()) {
            return;
        }
        if (key != null && key.location() != null) {
            if ("recipeeditor".equals(key.location().getNamespace())) {
                cir.setReturnValue(true);
                return;
            }
            if (CustomRecipeDispatcher.isIdentifierOverridden(key.location())) {
                cir.setReturnValue(false);
                return;
            }
        }
    }

    @Inject(method = "sendInitialRecipeBook", at = @At("RETURN"))
    private void onSendInitialRecipeBook(ServerPlayer player, CallbackInfo ci) {
        if (com.recipeeditor.RecipeEditorMod.isDedicatedServer()) {
            return;
        }
        CustomRecipeDispatcher.sendCustomRecipeBookEntries(player);
    }
}
"""
    Path("src/main/java/com/recipeeditor/mixin/ServerRecipeBookMixin.java").write_text(srb_src, encoding="utf-8")

    # CustomDynamicSmithingRecipe.java for <= 1.21.4
    smithing_src = """package com.recipeeditor.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SmithingRecipeDisplay;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public class CustomDynamicSmithingRecipe implements SmithingRecipe {
    private final String group;
    private final Optional<Ingredient> template;
    private final Optional<Ingredient> base;
    private final Optional<Ingredient> addition;
    private final ItemStack resultStack;
    private PlacementInfo placementInfo;

    public CustomDynamicSmithingRecipe(String group, Optional<Ingredient> template, Optional<Ingredient> base, Optional<Ingredient> addition, ItemStack resultStack) {
        this.group = group != null ? group : "";
        this.template = template != null && template.isPresent() && !template.get().isEmpty() ? template : Optional.empty();
        this.base = base != null && base.isPresent() && !base.get().isEmpty() ? base : Optional.empty();
        this.addition = addition != null && addition.isPresent() && !addition.get().isEmpty() ? addition : Optional.empty();
        this.resultStack = resultStack != null ? resultStack : ItemStack.EMPTY;
    }

    @Override
    public boolean matches(SmithingRecipeInput input, Level level) {
        if (input == null) return false;
        return Ingredient.testOptionalIngredient(this.template, input.template())
                && Ingredient.testOptionalIngredient(this.base, input.base())
                && Ingredient.testOptionalIngredient(this.addition, input.addition());
    }

    @Override
    public ItemStack assemble(SmithingRecipeInput input, HolderLookup.Provider registries) {
        if (this.resultStack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack baseStack = input != null ? input.base() : ItemStack.EMPTY;
        if (!baseStack.isEmpty()) {
            try {
                ItemStack applied = baseStack.transmuteCopy(this.resultStack.getItem(), this.resultStack.getCount());
                applied.applyComponents(this.resultStack.getComponentsPatch());
                if (applied != null && !applied.isEmpty()) {
                    return applied;
                }
            } catch (Throwable ignored) {}
        }
        return this.resultStack.copy();
    }

    @Override
    public Optional<Ingredient> templateIngredient() {
        return this.template;
    }

    @Override
    public Optional<Ingredient> baseIngredient() {
        return this.base;
    }

    @Override
    public Optional<Ingredient> additionIngredient() {
        return this.addition;
    }

    @Override
    public RecipeSerializer<? extends SmithingRecipe> getSerializer() {
        return RecipeSerializer.SMITHING_TRANSFORM;
    }

    @Override
    public RecipeType<SmithingRecipe> getType() {
        return RecipeType.SMITHING;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.SMITHING;
    }

    @Override
    public boolean showNotification() {
        return true;
    }

    @Override
    public String group() {
        return this.group;
    }

    @Override
    public PlacementInfo placementInfo() {
        if (this.placementInfo == null) {
            this.placementInfo = PlacementInfo.createFromOptionals(List.of(this.template, this.base, this.addition));
        }
        return this.placementInfo;
    }

    @Override
    public List<RecipeDisplay> display() {
        return List.of(new SmithingRecipeDisplay(
                Ingredient.optionalIngredientToDisplay(this.template),
                Ingredient.optionalIngredientToDisplay(this.base),
                Ingredient.optionalIngredientToDisplay(this.addition),
                new SlotDisplay.ItemStackSlotDisplay(this.resultStack),
                new SlotDisplay.ItemSlotDisplay(Items.SMITHING_TABLE)
        ));
    }
}
"""
    p_sm = Path("src/main/java/com/recipeeditor/recipe/CustomDynamicSmithingRecipe.java")
    if p_sm.exists():
        p_sm.write_text(smithing_src, encoding="utf-8")

    p_disp = Path("src/main/java/com/recipeeditor/recipe/CustomRecipeDispatcher.java")
    if p_disp.exists():
        t_disp = p_disp.read_text(encoding="utf-8")
        t_disp = t_disp.replace("defaultStack.has(DataComponents.WEAPON) ||", "item instanceof net.minecraft.world.item.SwordItem ||")
        p_disp.write_text(t_disp, encoding="utf-8")

    p_insp = Path("src/main/java/com/recipeeditor/inspector/RecipeInspector.java")
    if p_insp.exists():
        t_insp = p_insp.read_text(encoding="utf-8")
        t_insp = t_insp.replace("Ingredient baseIng = smithing.baseIngredient();\n            if (baseIng != null && !baseIng.isEmpty()) data.setSlotString(1, getSlotStringFromIngredient(baseIng));", "smithing.baseIngredient().ifPresent(ing -> data.setSlotString(1, getSlotStringFromIngredient(ing)));")
        p_insp.write_text(t_insp, encoding="utf-8")
    print("PoseStack and ServerRecipeBook adapted.")

def port_current_branch():
    mc_ver = get_minecraft_version()
    has_gametests = is_gametest_supported(mc_ver)
    uses_identifier = is_identifier_version(mc_ver)
    has_mouse_events = uses_mouse_events(mc_ver)
    has_posestack = uses_posestack(mc_ver)
    print(f"=== Porting {mc_ver} (GameTests: {has_gametests}, Identifier: {uses_identifier}, MouseEvents: {has_mouse_events}, PoseStack: {has_posestack}) ===")

    # 1. Checkout java code and build.gradle from 1.21.11
    subprocess.check_call(["git", "checkout", "1.21.11", "--", "src/main/java", "src/neoforge/java", "build.gradle"])
    if has_gametests:
        subprocess.check_call(["git", "checkout", "1.21.11", "--", "src/gametest"])

    # If gametests are not supported (1.21 - 1.21.4), adjust build.gradle
    if not has_gametests:
        p_build = Path("build.gradle")
        bg = p_build.read_text(encoding="utf-8")
        bg = re.sub(r"fabricApi\s*\{[^}]*\{[^}]*\}[^}]*\}\s*", "", bg)
        bg = re.sub(r"tasks\.matching\s*\{[^}]*runClientGameTest[^}]*\}[^}]*\}\s*", "", bg)
        p_build.write_text(bg, encoding="utf-8")

    # 2. Update gradle.properties: remove yarn_mappings
    p_props = Path("gradle.properties")
    props_lines = [l for l in p_props.read_text(encoding="utf-8").splitlines() if not l.startswith("yarn_mappings=")]
    p_props.write_text("\n".join(props_lines) + "\n", encoding="utf-8")

    # 3. If version uses ResourceLocation instead of Identifier (1.21 - 1.21.10)
    if not uses_identifier:
        print("Converting Identifier -> ResourceLocation for < 1.21.11...")
        for root, _, files in os.walk("src"):
            for f in files:
                if f.endswith(".java"):
                    p = Path(root) / f
                    t = p.read_text(encoding="utf-8")
                    t = t.replace("import net.minecraft.resources.Identifier;", "import net.minecraft.resources.ResourceLocation;")
                    t = t.replace("Identifier.", "ResourceLocation.")
                    t = t.replace("Identifier ", "ResourceLocation ")
                    t = t.replace("Identifier>", "ResourceLocation>")
                    t = t.replace("<Identifier", "<ResourceLocation")
                    t = t.replace("(Identifier", "(ResourceLocation")
                    t = t.replace("Identifier[]", "ResourceLocation[]")
                    t = t.replace(".identifier()", ".location()")
                    p.write_text(t, encoding="utf-8")

    # 4. If version uses classic GUI events (<= 1.21.8)
    if not has_mouse_events:
        adapt_classic_gui()

    # 5. If version uses PoseStack / older ServerRecipeBook (<= 1.21.5)
    if has_posestack:
        adapt_posestack_and_recipe_book()

    if uses_ingredient_items(mc_ver):
        print("Adapting ingredient.items() (<= 1.21.3)...")
        # CustomRecipeData
        p_crd = Path("src/main/java/com/recipeeditor/config/CustomRecipeData.java")
        t_crd = p_crd.read_text(encoding="utf-8")
        t_crd = t_crd.replace("cachedIngredients[slot].isEmpty()", "cachedIngredients[slot].items().isEmpty()")
        t_crd = t_crd.replace("retry.isEmpty()", "retry.items().isEmpty()")
        t_crd = t_crd.replace("if (ing != null && !ing.isEmpty())", "if (ing != null && !ing.items().isEmpty())")
        p_crd.write_text(t_crd, encoding="utf-8")

        # RecipeInspector
        p_ins = Path("src/main/java/com/recipeeditor/inspector/RecipeInspector.java")
        t_ins = p_ins.read_text(encoding="utf-8")
        t_ins = t_ins.replace('if (ing == null || ing.isEmpty()) return "minecraft:air";', 'if (ing == null || ing.items().isEmpty()) return "minecraft:air";')
        p_ins.write_text(t_ins, encoding="utf-8")

        # RecipeEditorDynamicDisplayGenerator
        p_gen = Path("src/main/java/com/recipeeditor/integration/rei/RecipeEditorDynamicDisplayGenerator.java")
        t_gen = p_gen.read_text(encoding="utf-8")
        t_gen = t_gen.replace("if (ing != null && !ing.isEmpty())", "if (ing != null && !ing.items().isEmpty())")
        p_gen.write_text(t_gen, encoding="utf-8")

        # CustomDynamicSmithingRecipe
        p_sm = Path("src/main/java/com/recipeeditor/recipe/CustomDynamicSmithingRecipe.java")
        t_sm = p_sm.read_text(encoding="utf-8")
        t_sm = t_sm.replace("!template.get().isEmpty()", "!template.get().items().isEmpty()")
        t_sm = t_sm.replace("!base.get().isEmpty()", "!base.get().items().isEmpty()")
        t_sm = t_sm.replace("!addition.get().isEmpty()", "!addition.get().items().isEmpty()")
        p_sm.write_text(t_sm, encoding="utf-8")

        # CustomRecipeDispatcher
        p_dp = Path("src/main/java/com/recipeeditor/recipe/CustomRecipeDispatcher.java")
        t_dp = p_dp.read_text(encoding="utf-8")
        t_dp = t_dp.replace("if (t != null && !t.isEmpty()) templates.add(t);", "if (t != null && !t.items().isEmpty()) templates.add(t);")
        t_dp = t_dp.replace("if (b != null && !b.isEmpty()) bases.add(b);", "if (b != null && !b.items().isEmpty()) bases.add(b);")
        t_dp = t_dp.replace("if (a != null && !a.isEmpty()) additions.add(a);", "if (a != null && !a.items().isEmpty()) additions.add(a);")
        t_dp = t_dp.replace("if (baseIng == null || baseIng.isEmpty()) return false;", "if (baseIng == null || baseIng.items().isEmpty()) return false;")
        t_dp = t_dp.replace("templateIng.isEmpty()", "templateIng.items().isEmpty()")
        t_dp = t_dp.replace("additionIng.isEmpty()", "additionIng.items().isEmpty()")
        t_dp = t_dp.replace("if (ing == null || ing.isEmpty()) return null;", "if (ing == null || ing.items().isEmpty()) return null;")
        t_dp = t_dp.replace("baseIng.get().isEmpty()", "baseIng.get().items().isEmpty()")
        p_dp.write_text(t_dp, encoding="utf-8")

        # RecipeInspectorTest
        p_test = Path("src/gametest/java/com/recipeeditor/test/RecipeInspectorTest.java")
        if p_test.exists():
            t_test = p_test.read_text(encoding="utf-8")
            t_test = t_test.replace('check(ing.isEmpty(), "computeIngredientForSlot for unknown tag must return an empty ingredient");', 'check(ing.items().isEmpty(), "computeIngredientForSlot for unknown tag must return an empty ingredient");')
            p_test.write_text(t_test, encoding="utf-8")

    # 6. Verify compilation
    print("Running ./gradlew compileJava...")
    subprocess.check_call(["cmd.exe", "/c", "gradlew.bat", "compileJava"])
    if has_gametests:
        print("Running ./gradlew compileGametestJava...")
        subprocess.check_call(["cmd.exe", "/c", "gradlew.bat", "compileGametestJava"])
    print("Running ./gradlew build...")
    subprocess.check_call(["cmd.exe", "/c", "gradlew.bat", "build"])

    # 7. Commit
    subprocess.check_call(["git", "add", "."])
    commit_msg = f"Migrate {mc_ver} from Yarn to Official Mojang mappings"
    if has_gametests:
        commit_msg += " and add 7-suite GameTests"
    subprocess.check_call(["git", "commit", "-m", commit_msg])
    print(f"Successfully committed {mc_ver}!")

if __name__ == "__main__":
    port_current_branch()
