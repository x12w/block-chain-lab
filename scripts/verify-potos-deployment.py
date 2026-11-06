#!/usr/bin/env python3
"""Read-only verification of saved route A HTTP responses; never loads a private key."""
import datetime
import json
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]
RECORDS = ROOT / 'evidence/potos/records'
RPC = 'https://rpc-testnet.potos.hk'
BACKEND = '0x4c688b060912faf95324a43C0190C59E7cE8aB3c'


def rpc(method, params):
    result = subprocess.run(['curl', '--fail-with-body', '--silent', '--show-error', '--max-time', '30',
                             RPC, '-H', 'Content-Type: application/json', '--data-binary', '@-'],
                            input=json.dumps({'jsonrpc': '2.0', 'id': 1, 'method': method, 'params': params}),
                            text=True, capture_output=True, check=True)
    payload = json.loads(result.stdout)
    if 'error' in payload:
        raise RuntimeError(payload['error'])
    return payload['result']


def main():
    deploy = json.loads((RECORDS / '04-escrow-deploy.json').read_text())
    storage = json.loads((RECORDS / '02-storage-set100.json').read_text())
    assert int(rpc('eth_chainId', []), 16) == 60600
    receipts = {}
    for label, response in [('escrowDeploy', deploy), ('storageSet100', storage)]:
        receipt = rpc('eth_getTransactionReceipt', [response['transactionHash']])
        assert receipt and receipt['status'] == '0x1'
        assert receipt['from'].lower() == BACKEND.lower()
        receipts[label] = receipt
    assert receipts['escrowDeploy']['contractAddress'].lower() == deploy['contractAddress'].lower()
    assert int(receipts['escrowDeploy']['blockNumber'], 16) == int(deploy['deployBlock'])
    assert receipts['storageSet100']['to'].lower() == storage['contractAddress'].lower()
    arbiter = rpc('eth_call', [{'to': deploy['contractAddress'], 'data': '0xfe25e00a'}, 'latest'])
    assert arbiter[-40:].lower() == BACKEND[2:].lower()
    head = int(rpc('eth_blockNumber', []), 16)
    balance = int(rpc('eth_getBalance', [BACKEND, 'latest']), 16)
    result = {
        'classification': 'POTOS_TESTNET_DEPLOYED_GROUP_ORDER_ACCEPTANCE_PENDING',
        'chainId': 60600, 'rpc': RPC,
        'verifiedAt': datetime.datetime.now(datetime.timezone(datetime.timedelta(hours=8))).isoformat(),
        'observedBlock': head, 'backendAccount': BACKEND, 'backendBalanceWei': str(balance),
        'escrow': deploy, 'simpleStorage': storage,
        'explorerBaseUrl': 'https://scan-testnet.potos.hk',
        'privateKeyStoredInRepository': False,
    }
    (ROOT / 'docs/route-a-deployment.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
    (RECORDS / 'verified-receipts.json').write_text(json.dumps(receipts, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
