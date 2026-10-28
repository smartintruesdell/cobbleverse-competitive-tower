"""Copy only exact, locally supplied dependencies. Never downloads proprietary mods."""
import hashlib
import json
from pathlib import Path
import shutil
import sys
import zipfile

root = Path(__file__).resolve().parents[1]
mods = Path(sys.argv[1])
required = {"cobblemon", "cobblemon_battle_tower", "rctapi", "architectury", "wild_battle_api"}
pins = json.loads((root / "docs/dependency-pins.json").read_text())
(root / "libs/support").mkdir(parents=True, exist_ok=True)
for pin in pins:
    if pin["id"] not in required:
        continue
    source = mods / pin["file"]
    if hashlib.sha256(source.read_bytes()).hexdigest() != pin["sha256"]:
        raise SystemExit(f"Hash mismatch: {source.name}; re-inspection required")
    shutil.copy2(source, root / "libs" / source.name)
    if pin["id"] == "cobblemon":
        with zipfile.ZipFile(source) as jar:
            for name in jar.namelist():
                if name.startswith("META-INF/jars/") and name.endswith(".jar"):
                    (root / "libs/support" / Path(name).name).write_bytes(jar.read(name))
    required.remove(pin["id"])
if required:
    raise SystemExit(f"Missing pins: {required}")
print("Verified and prepared local dependencies. These JARs must not be redistributed.")
