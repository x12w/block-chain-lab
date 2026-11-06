#!/usr/bin/env python3
"""Summarize actual JUnit XML and chain receipts, without inventing acceptance results."""
import json
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
print('ROUTE A / MEMBER 1 / LOCAL VALIDATION (chain 31337)')
print('This is a single-process test, not three-person POTOS acceptance.')
reports = sorted((root / 'build/test-results').glob('*/TEST-*.xml'))
if not reports:
    raise SystemExit('No test reports: run test and integrationTest first')
total = failures = errors = skipped = 0
suites = set()
for path in reports:
    suite = ET.parse(path).getroot()
    suites.add(suite.attrib['name'])
    total += int(suite.attrib['tests'])
    failures += int(suite.attrib['failures'])
    errors += int(suite.attrib['errors'])
    skipped += int(suite.attrib.get('skipped', 0))
    print(f"{suite.attrib['name']}: tests={suite.attrib['tests']} failures={suite.attrib['failures']} errors={suite.attrib['errors']}")
print(f'TOTAL tests={total}, failures={failures}, errors={errors}, skipped={skipped}')
evidence = json.loads((root / 'build/local-evidence/local-integration.json').read_text())
print('Recorded:', evidence['recordedAtUtc'])
print('Escrow:', evidence['escrowAddress'])
print('Deploy block:', evidence['deployBlock'])
print('Final state:', json.dumps(evidence['finalState']))
for tx in evidence['transactions']:
    if tx['status'] == '0x0' or tx['operation'] in {'resolve 1 refund', 'confirm 0'}:
        print(f"{tx['operation']} | block={tx['block']} status={tx['status']} gas={tx['gasUsed']}")
        print('tx=' + tx['hash'])
required_suites = {
    'com.wetech.demo.web3j.service.EscrowServiceTest',
    'com.wetech.demo.web3j.controller.EscrowControllerTest',
    'com.wetech.demo.web3j.service.EscrowServiceIntegrationTest',
    'com.wetech.demo.web3j.controller.EscrowHttpIntegrationTest',
}
http = json.loads((root / 'build/local-evidence/http-integration.json').read_text())
print('HTTP exchanges:', len(http['httpExchanges']))
print('HTTP final state:', json.dumps(http['finalState']))
if failures or errors or skipped or not required_suites.issubset(suites):
    raise SystemExit('Validation is incomplete or failed')
print('PASS: all expected tests completed; raw receipts are preserved in build/local-evidence/')
