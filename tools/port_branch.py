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
        return parts[2] >= 5
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

def port_current_branch():
    mc_ver = get_minecraft_version()
    has_gametests = is_gametest_supported(mc_ver)
    uses_identifier = is_identifier_version(mc_ver)
    has_mouse_events = uses_mouse_events(mc_ver)
    print(f"=== Porting {mc_ver} (GameTests: {has_gametests}, Identifier: {uses_identifier}, MouseEvents: {has_mouse_events}) ===")

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

    # 5. Verify compilation
    print("Running ./gradlew compileJava...")
    subprocess.check_call(["cmd.exe", "/c", "gradlew.bat", "compileJava"])
    if has_gametests:
        print("Running ./gradlew compileGametestJava...")
        subprocess.check_call(["cmd.exe", "/c", "gradlew.bat", "compileGametestJava"])
    print("Running ./gradlew build...")
    subprocess.check_call(["cmd.exe", "/c", "gradlew.bat", "build"])

    # 6. Commit
    subprocess.check_call(["git", "add", "."])
    commit_msg = f"Migrate {mc_ver} from Yarn to Official Mojang mappings"
    if has_gametests:
        commit_msg += " and add 7-suite GameTests"
    subprocess.check_call(["git", "commit", "-m", commit_msg])
    print(f"Successfully committed {mc_ver}!")

if __name__ == "__main__":
    port_current_branch()
