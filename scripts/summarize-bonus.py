#!/usr/bin/env python3
"""Archive completed local JUnit results with the exact source snapshot."""
from pathlib import Path
import hashlib
import json
import shutil
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
out = root / 'evidence/bonus/enhanced'
out.mkdir(parents=True, exist_ok=True)
suites = []
for path in sorted((root / 'build/test-results').glob('*/*.xml')):
    suite = ET.parse(path).getroot()
    suites.append({key: suite.attrib[key] for key in
                   ('name', 'tests', 'failures', 'errors', 'skipped', 'timestamp')})
    destination = out / 'junit' / path.parent.name / path.name
    destination.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(path, destination)
assert len(suites) == 6, 'Expected all six test suites'
assert sum(int(x['tests']) for x in suites) == 24
assert all(int(x[key]) == 0 for x in suites for key in ('failures', 'errors', 'skipped'))
for name in ('shipping', 'multisig', 'boundaries'):
    source = root / f'build/local-evidence/enhanced-{name}.json'
    record = json.loads(source.read_text())
    assert record['chainId'] == 31337
    shutil.copyfile(source, out / source.name)
for name in ('http-integration.json', 'local-integration.json'):
    shutil.copyfile(root / 'build/local-evidence' / name, out / name)
sources = [root / 'build.gradle']
sources += sorted((root / 'src').rglob('*.java'))
sources += sorted((root / 'src/main/resources/contracts').glob('*.sol'))
summary = {
    'classification': 'LOCAL_EVM_HTTP_ACCEPTANCE_NOT_POTOS',
    'tests': suites,
    'sourceSha256': {str(p.relative_to(root)): hashlib.sha256(p.read_bytes()).hexdigest()
                     for p in sources},
    'totalTests': 24,
    'failed': 0,
}
(out / 'summary.json').write_text(json.dumps(summary, indent=2) + '\n')
print('PASS: 19 unit tests + 5 local integration tests; receipts and source hashes archived')
