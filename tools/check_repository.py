"""Check the Git-tracked source boundary before publishing. Does not read untracked secrets."""
from pathlib import Path
import re
import subprocess
import sys

root = Path(__file__).resolve().parents[1]
paths = subprocess.check_output(['git', 'ls-files', '-z'], cwd=root).decode().split('\0')
errors = []
for name in filter(None, paths):
    path = Path(name)
    if path.parts[0] in {'libs', 'build', 'run', 'deployment', 'logs', 'config', 'saves', 'world', '.gradle', '.mixin.out', 'decompiled', 'decompilations'}:
        errors.append(f'Private/generated path: {name}')
    if 'hostealo' in name.lower() or name == 'docs/installed-mods.json':
        errors.append(f'Private operations/inventory: {name}')
    if path.suffix in {'.jar', '.class', '.zip'} and name != 'gradle/wrapper/gradle-wrapper.jar':
        errors.append(f'Unexpected binary/archive: {name}')
    data = (root / name).read_bytes()
    if path.suffix in {'.jar', '.png'}:
        continue
    try:
        text = data.decode('utf-8')
    except UnicodeDecodeError:
        errors.append(f'Unexpected non-text file: {name}')
        continue
    for pattern in [r'/[U]sers/[^/\s]+/', r'[A]KIA[0-9A-Z]{16}', r'g[h]p_[A-Za-z0-9]{30,}', r'github_[p]at_[A-Za-z0-9_]{30,}', r'-----BEGIN (?:[A-Z]+ )?PRIVATE [K]EY-----']:
        if re.search(pattern, text):
            errors.append(f'Private-machine path or credential pattern: {name}')
            break
if errors:
    print('\n'.join(errors), file=sys.stderr)
    raise SystemExit(1)
print(f'Checked {len(list(filter(None, paths)))} tracked files: source boundary passed.')
