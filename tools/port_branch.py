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
    # Supported from 1.21.5 onwards and 26.x
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

def port_current_branch():
    mc_ver = get_minecraft_version()
    has_gametests = is_gametest_supported(mc_ver)
    uses_identifier = is_identifier_version(mc_ver)
    print(f"=== Porting {mc_ver} (GameTests: {has_gametests}, Uses Identifier: {uses_identifier}) ===")

    # 1. Checkout java code and build.gradle from 1.21.11
    subprocess.check_call(["git", "checkout", "1.21.11", "--", "src/main/java", "src/neoforge/java", "build.gradle"])
    if has_gametests:
        subprocess.check_call(["git", "checkout", "1.21.11", "--", "src/gametest"])

    # If gametests are not supported (1.21 - 1.21.4), adjust build.gradle
    if not has_gametests:
        p_build = Path("build.gradle")
        bg = p_build.read_text(encoding="utf-8")
        # Remove fabricApi configureTests block
        bg = re.sub(r"fabricApi\s*\{[^}]*\{[^}]*\}[^}]*\}\s*", "", bg)
        # Remove runClientGameTest block
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

    # 4. Verify compilation
    print("Running ./gradlew compileJava...")
    subprocess.check_call(["cmd.exe", "/c", "gradlew.bat", "compileJava"])
    if has_gametests:
        print("Running ./gradlew compileGametestJava...")
        subprocess.check_call(["cmd.exe", "/c", "gradlew.bat", "compileGametestJava"])
    print("Running ./gradlew build...")
    subprocess.check_call(["cmd.exe", "/c", "gradlew.bat", "build"])

    # 5. Commit
    subprocess.check_call(["git", "add", "."])
    commit_msg = f"Migrate {mc_ver} from Yarn to Official Mojang mappings"
    if has_gametests:
        commit_msg += " and add 7-suite GameTests"
    subprocess.check_call(["git", "commit", "-m", commit_msg])
    print(f"Successfully committed {mc_ver}!")

if __name__ == "__main__":
    port_current_branch()
