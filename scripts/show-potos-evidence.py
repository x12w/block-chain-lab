#!/usr/bin/env python3
"""Display previously saved real curl responses for screenshots; does not send transactions."""
import json
from pathlib import Path
import sys

records = Path(__file__).resolve().parents[1] / 'evidence/potos/records'
mode = sys.argv[1] if len(sys.argv) > 1 else 'storage'
groups = {
    'storage': [
        ('POST', '/api/storage/deploy', '01-storage-deploy.json'),
        ('POST', '/api/storage/value/set?value=100', '02-storage-set100.json'),
        ('GET', '/api/storage/value/get', '03-storage-get.json'),
    ],
    'escrow': [
        ('POST', '/api/escrow/deploy', '04-escrow-deploy.json'),
        ('GET', '/api/escrow/address', '05-escrow-address.json'),
    ],
}
print('POTOS TESTNET / chain 60600 / SAVED REAL CURL RESPONSES')
print('Read from evidence/potos/records; no transaction is repeated by this display.')
for method, path, filename in groups[mode]:
    print(f'\nRecorded request: curl -X {method} "http://127.0.0.1:8080{path}"')
    print(json.dumps(json.loads((records / filename).read_text()), indent=2))
