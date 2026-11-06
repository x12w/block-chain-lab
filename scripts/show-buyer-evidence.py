#!/usr/bin/env python3
"""Display saved, independently verified POTOS records for a labelled terminal capture."""
import json
from pathlib import Path
import sys

records = Path(__file__).resolve().parents[1] / 'evidence/potos/records'
report = json.loads((records / 'buyer-verification.json').read_text())
print('POTOS TESTNET | SAVED READ-ONLY VERIFICATION | chainId = 60600')
print('Verified at:', report['verifiedAt'], '| snapshot block:', report['observedBlock'])
print('Escrow:', report['escrow'])
print('Token :', report['token'])
print('Buyer :', report['orders'][0]['buyer'])
print('Seller:', report['orders'][0]['seller'])
print('Arbiter:', report['arbiter'])
print()
if len(sys.argv) > 1 and sys.argv[1] == 'transactions':
    for tx in report['transactions']:
        print(f"{tx['label']:15} status={tx['receiptStatus']}  block={tx['blockNumber']}  gas={tx['gasUsed']}")
        print('  tx:', tx['transactionHash'])
        print('  events:', ', '.join(event['name'] for event in tx['events']))
else:
    for order in report['orders']:
        print(f"Order {order['id']}: {order['status']} ({order['statusCode']}) | amount={order['amount']}")
        print('  description:', order['description'])
        print('  deadline (Asia/Shanghai):', order['deadlineBeijing'])
    print()
    print('WQLToken balance (18 decimals):', report['tokenBalances'])
    print('Buyer allowance to Escrow:', report['buyerAllowanceTokens'])
    print('Next: SELLER dispute(1); keep order 0 Paid for failed arbitration.')
    print('This Paid/Paid snapshot does NOT replace the later Paid/Disputed 4A-6 capture.')
print()
print('Source: evidence/potos/records/buyer-verification.json + buyer-verified-receipts.json')
