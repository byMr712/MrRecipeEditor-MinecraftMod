from pathlib import Path

p = Path("build.gradle")
c = p.read_text(encoding="utf-8")

old_part = """    if (fabricApi) {
        modLocalRuntime "net.fabricmc.fabric-api:fabric-api:${fabricApi}"
        gametestImplementation "net.fabricmc.fabric-api:fabric-api:${fabricApi}" 
    }"""

new_part = """    if (fabricApi) {
        modImplementation "net.fabricmc.fabric-api:fabric-api:${fabricApi}"
    }"""

if old_part in c:
    c = c.replace(old_part, new_part)
    p.write_text(c, encoding="utf-8")
    print("build.gradle patched!")
else:
    # Try normalizing CRLF
    old_crlf = old_part.replace("\n", "\r\n")
    new_crlf = new_part.replace("\n", "\r\n")
    if old_crlf in c:
        c = c.replace(old_crlf, new_crlf)
        p.write_text(c, encoding="utf-8")
        print("build.gradle patched (CRLF)!")
    else:
        print("Could not find old_part in build.gradle")
