#!/usr/bin/env python3
"""Conservative source scan. Print locations, never potential secret values."""
import json
import hashlib
from pathlib import Path
import re
import subprocess
import sys

root = Path(__file__).resolve().parents[1]
reference = root / 'docs/lab3-reference.json'
public_hashes = {v['transactionHash'] for v in json.loads(reference.read_text())['contracts'].values()}
evidence = root / 'evidence/member1'
local_records = evidence / 'records/06-local-integration.json'
local_transaction_hashes = ({tx['hash'] for tx in json.loads(local_records.read_text())['transactions']}
                            if local_records.exists() else set())
integration_evidence = root / 'evidence/pr1-integration'
integration_hashes = set()
for record_file in (integration_evidence / 'records').glob('*integration.json'):
    integration_hashes.update(tx['hash'] for tx in json.loads(record_file.read_text())['transactions'])
potos_evidence = root / 'evidence/potos'
potos_public_values = set()
for potos_receipts in (potos_evidence / 'records').glob('*verified-receipts.json'):
    for receipt in json.loads(potos_receipts.read_text()).values():
        potos_public_values.update(receipt[field] for field in ('transactionHash', 'blockHash'))
        for log in receipt.get('logs', []):
            potos_public_values.update(log['topics'])
            potos_public_values.update(log[field] for field in ('transactionHash', 'blockHash', 'data'))
hex_value = re.compile(r'(?<![a-zA-Z0-9])(?:0x)?[0-9a-fA-F]{64}(?![a-zA-Z0-9])')
verified_manifest_digests = {}
for manifest in potos_evidence.rglob('source-manifest.json'):
    digests = set()
    for entry in json.loads(manifest.read_text()).get('files', []):
        source = (manifest.parent / entry['file']).resolve()
        if source.is_relative_to(manifest.parent.resolve()) and source.is_file():
            digest = hashlib.sha256(source.read_bytes()).hexdigest()
            if digest == entry.get('sha256'):
                digests.add(digest)
    verified_manifest_digests[manifest] = digests
findings = []
checked = 0
bonus_public_values = set()

def collect_receipt_values(value):
    if isinstance(value, dict):
        for key, item in value.items():
            if key in {'hash', 'transactionHash', 'blockHash'} and isinstance(item, str):
                if re.fullmatch(r'0x[0-9a-fA-F]{64}', item):
                    bonus_public_values.add(item)
            collect_receipt_values(item)
    elif isinstance(value, list):
        for item in value:
            collect_receipt_values(item)

bonus_evidence = root / 'evidence/bonus'
for folder in ('enhanced', 'timeline'):
    for record in (bonus_evidence / folder).glob('*.json'):
        data = json.loads(record.read_text())
        collect_receipt_values(data)
        for relative, recorded in data.get('sourceSha256', {}).items():
            source = (root / relative).resolve()
            if source.is_relative_to(root) and source.is_file():
                actual = hashlib.sha256(source.read_bytes()).hexdigest()
                if actual == recorded:
                    bonus_public_values.add(actual)

# Scan the same tracked/staged file set that is included in the delivery archive.
# Local untracked work is not published or silently included in this check.
tracked = subprocess.check_output(['git', 'ls-files', '-z'], cwd=root).decode().split('\0')
for path in sorted(root / name for name in tracked if name):
    if not path.is_file() or any(p in {'.git', '.gradle', 'build', 'node_modules'} for p in path.relative_to(root).parts):
        continue
    if path.name == '.env' or (path.name.startswith('.env.') and path.name != '.env.example'):
        findings.append(f'{path.relative_to(root)}: runtime environment file must not be submitted')
        continue
    try:
        text = path.read_text()
    except (UnicodeDecodeError, OSError):
        continue
    checked += 1
    for line_no, line in enumerate(text.splitlines(), 1):
        for match in hex_value.finditer(line):
            if match.group() in verified_manifest_digests.get(path, set()):
                continue  # Recomputed file digest, not an arbitrary manifest exemption.
            if path == reference and match.group() in public_hashes:
                continue  # Independently documented public transaction hashes, not credentials.
            if path.is_relative_to(evidence) and match.group() in local_transaction_hashes:
                continue  # Actual local-chain receipt hashes, also present in the test output.
            if path.is_relative_to(integration_evidence) and match.group() in integration_hashes:
                continue  # Recorded HTTP/service local-chain transaction receipts.
            if path.is_relative_to(bonus_evidence) and match.group() in bonus_public_values:
                continue  # Typed public receipt hashes and independently recomputed source digests.
            if (path.is_relative_to(potos_evidence) or path.is_relative_to(root / 'docs')
                    or path == root / 'README.md') and match.group() in potos_public_values:
                continue  # Public RPC receipt fields; exact values, not a blanket JSON exemption.
            findings.append(f'{path.relative_to(root)}:{line_no}: potential key (value omitted)')
        if path.suffix == '.properties' and re.match(r'\s*web3j\.private-key\s*=', line):
            if line.split('=', 1)[1].strip() != '${WEB3J_PRIVATE_KEY}':
                findings.append(f'{path.relative_to(root)}:{line_no}: private key must be an environment placeholder')

print(f'Tracked/staged submission text files checked: {checked}')
print('Public historical transaction hashes: explicitly classified in docs/lab3-reference.json')
print('Local transaction hashes: explicitly classified in evidence/member1/records/06-local-integration.json')
print('PR integration hashes: explicitly classified in evidence/pr1-integration/records/*integration.json')
print('POTOS public receipt fields: explicitly classified in evidence/potos/records/*verified-receipts.json')
print('Bonus public receipt hashes and source digests: classified from archived JSON and actual source bytes')
if findings:
    print('\n'.join(findings))
    sys.exit(1)
print('PASS: no candidate plaintext private key or runtime .env file in submission sources')
print('Excluded build/cache directories are also excluded from the delivery archive.')
