with open('build.gradle', 'r', encoding='utf-8') as f:
    c = f.read()

target1 = 'modLocalRuntime "net.fabricmc.fabric-api:fabric-api:${fabricApi}"'
repl1 = '''modLocalRuntime "net.fabricmc.fabric-api:fabric-api:${fabricApi}"
        gametestImplementation "net.fabricmc.fabric-api:fabric-api:${fabricApi}"'''

target2 = 'def targetJavaVersion ='
repl2 = '''tasks.matching { it.name == 'runClientGameTest' }.configureEach {
    jvmArgs '-Dfabric.client.gametest.disableNetworkSynchronizer=true'
}

def targetJavaVersion ='''

if target1 in c:
    c = c.replace(target1, repl1, 1)
if target2 in c:
    c = c.replace(target2, repl2, 1)

with open('build.gradle', 'w', encoding='utf-8') as f:
    f.write(c)

print("Updated build.gradle successfully")
