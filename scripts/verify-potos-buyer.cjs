#!/usr/bin/env node
// Read-only buyer verification. ETHERS_MODULE may point to an existing ethers installation.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const {execFileSync} = require('node:child_process');
const {Interface, formatUnits} = require(process.env.ETHERS_MODULE || 'ethers');
const root = path.resolve(__dirname, '..');
const records = path.join(root, 'evidence/potos/records');
const input = JSON.parse(fs.readFileSync(path.join(records, 'buyer-reported-transactions.json')));
const escrowAbi = new Interface(JSON.parse(fs.readFileSync(path.join(root, 'build/resources/main/solidity/Escrow.abi'))));
const tokenAbi = new Interface(JSON.parse(fs.readFileSync(path.join(root, 'build/resources/main/solidity/WQLToken.abi'))));
const normalize = value => typeof value === 'bigint' ? value.toString() : value;
const sameAddress = (actual, expected) => assert.equal(actual.toLowerCase(), expected.toLowerCase());
function rpc(method, params) {
  const response = JSON.parse(execFileSync('curl', ['--fail-with-body', '--silent', '--show-error',
    '--max-time', '30', '--tlsv1.2', '--tls-max', '1.2', '--retry', '2', '--retry-all-errors',
    input.rpc, '-H', 'Content-Type: application/json', '--data-binary', '@-'],
    {input: JSON.stringify({jsonrpc: '2.0', id: 1, method, params}), encoding: 'utf8'}));
  if (response.error) throw new Error(JSON.stringify(response.error));
  return response.result;
}
function call(address, abi, name, args, block) {
  return abi.decodeFunctionResult(name, rpc('eth_call', [{to: address, data: abi.encodeFunctionData(name, args)}, block]));
}
function decodedLogs(receipt) {
  return receipt.logs.map(log => {
    const abi = log.address.toLowerCase() === input.escrow.toLowerCase() ? escrowAbi : tokenAbi;
    const parsed = abi.parseLog(log);
    assert(parsed, 'Unexpected undecodable event');
    const args = Object.fromEntries(parsed.fragment.inputs.map((item, i) => [item.name, normalize(parsed.args[i])]));
    return {address: log.address, name: parsed.name, args};
  });
}
const receipts = {}, transactions = [];
assert.equal(Number(BigInt(rpc('eth_chainId', []))), 60600);
for (const item of input.transactions) {
  const receipt = rpc('eth_getTransactionReceipt', [item.hash]);
  const tx = rpc('eth_getTransactionByHash', [item.hash]);
  assert(receipt && tx, 'Transaction must be mined');
  assert.equal(receipt.status, '0x1');
  assert.equal(receipt.transactionHash, item.hash);
  sameAddress(receipt.from, input.buyer);
  sameAddress(tx.from, input.buyer);
  assert.equal(BigInt(tx.value), 0n);
  const isApprove = item.method === 'approve';
  sameAddress(receipt.to, isApprove ? input.token : input.escrow);
  sameAddress(tx.to, receipt.to);
  const abi = isApprove ? tokenAbi : escrowAbi;
  const parsed = abi.parseTransaction({data: tx.input, value: tx.value});
  assert.equal(parsed.name, item.method);
  const args = Object.fromEntries(parsed.fragment.inputs.map((arg, i) => [arg.name, normalize(parsed.args[i])]));
  const events = decodedLogs(receipt);
  const block = rpc('eth_getBlockByNumber', [receipt.blockNumber, false]);
  if (isApprove) {
    sameAddress(parsed.args[0], input.escrow);
    assert.equal(parsed.args[1], 60n * 10n ** 18n);
    const approval = events.find(event => event.name === 'Approval');
    assert(approval);
    sameAddress(approval.args.owner, input.buyer);
    sameAddress(approval.args.spender, input.escrow);
    assert.equal(approval.args.value, '60000000000000000000');
  } else if (item.method === 'createOrder') {
    sameAddress(args.seller, input.seller);
    sameAddress(args.token, input.token);
    assert.equal(args.amount, '30000000000000000000');
    assert.equal(args.description, item.description);
    assert(BigInt(args.deadline) > BigInt(block.timestamp));
    const created = events.find(event => event.name === 'OrderCreated');
    assert(created);
    assert.equal(created.args.id, String(item.orderId));
    sameAddress(created.args.buyer, input.buyer);
    sameAddress(created.args.seller, input.seller);
    sameAddress(created.args.token, input.token);
    assert.equal(created.args.amount, args.amount);
    assert.equal(created.args.deadline, args.deadline);
  } else {
    assert.equal(args.id, String(item.orderId));
    assert(events.some(event => event.name === 'OrderPaid' && event.args.id === String(item.orderId)));
    const transfer = events.find(event => event.name === 'Transfer');
    assert(transfer);
    sameAddress(transfer.args.from, input.buyer);
    sameAddress(transfer.args.to, input.escrow);
    assert.equal(transfer.args.value, '30000000000000000000');
  }
  receipts[item.label] = receipt;
  transactions.push({label: item.label, transactionHash: item.hash, from: tx.from, to: tx.to,
    method: parsed.name, arguments: args, blockNumber: Number(BigInt(receipt.blockNumber)),
    blockTime: new Date(Number(BigInt(block.timestamp)) * 1000).toISOString(),
    receiptStatus: receipt.status, gasUsed: BigInt(receipt.gasUsed).toString(), events});
  console.log(`Verified ${item.label}: block ${Number(BigInt(receipt.blockNumber))}, status ${receipt.status}`);
}
fs.writeFileSync(path.join(records, 'buyer-verified-receipts.json'), JSON.stringify(receipts, null, 2) + '\n');
const blockTag = rpc('eth_blockNumber', []);
const block = rpc('eth_getBlockByNumber', [blockTag, false]);
const arbiter = call(input.escrow, escrowAbi, 'arbiter', [], blockTag)[0];
sameAddress(arbiter, input.arbiter);
const orderCount = Number(call(input.escrow, escrowAbi, 'orderCount', [], blockTag)[0]);
assert.equal(orderCount, 2);
const statusNames = ['Created', 'Paid', 'Completed', 'Refunded', 'Disputed'];
const orders = [0, 1].map(id => {
  const order = call(input.escrow, escrowAbi, 'getOrder', [id], blockTag)[0];
  sameAddress(order.buyer, input.buyer);
  sameAddress(order.seller, input.seller);
  sameAddress(order.token, input.token);
  assert.equal(order.amount, 30n * 10n ** 18n);
  return {id, buyer: order.buyer, seller: order.seller, token: order.token, amount: order.amount.toString(),
    deadline: order.deadline.toString(), deadlineBeijing: new Date(Number(order.deadline) * 1000).toLocaleString('sv-SE', {timeZone: 'Asia/Shanghai'}),
    statusCode: Number(order.status), status: statusNames[Number(order.status)], description: order.description};
});
assert.equal(call(input.token, tokenAbi, 'decimals', [], blockTag)[0], 18n);
const balances = Object.fromEntries(['buyer', 'seller', 'escrow'].map(role => [role,
  formatUnits(call(input.token, tokenAbi, 'balanceOf', [input[role]], blockTag)[0], 18)]));
const allowance = formatUnits(call(input.token, tokenAbi, 'allowance', [input.buyer, input.escrow], blockTag)[0], 18);
const report = {classification: 'POTOS_TESTNET_BUYER_TRANSACTIONS_INDEPENDENTLY_VERIFIED',
  verifiedAt: new Date().toISOString(), rpc: input.rpc, chainId: 60600, observedBlock: Number(BigInt(blockTag)),
  observedBlockTime: new Date(Number(BigInt(block.timestamp)) * 1000).toISOString(),
  escrow: input.escrow, token: input.token, arbiter, orderCount, orders, tokenBalances: balances,
  buyerAllowanceTokens: allowance, transactions, pending: ['seller_dispute_order_1', 'REST_Paid_Disputed_queries',
    'backend_failed_resolve_order_0', 'backend_refund_order_1', 'buyer_confirm_order_0'],
  originalRemixScreenshots: {sourceConversation: '6aedd0e7-c6d0-83e8-b116-0d73d84996bf',
    remoteArtifactName: 'route_a_buyer_progress.zip', importedIntoRepository: false}};
fs.writeFileSync(path.join(records, 'buyer-verified-receipts.json'), JSON.stringify(receipts, null, 2) + '\n');
fs.writeFileSync(path.join(records, 'buyer-verification.json'), JSON.stringify(report, null, 2) + '\n');
console.log(JSON.stringify({observedBlock: report.observedBlock, orders, tokenBalances: balances, buyerAllowanceTokens: allowance}, null, 2));
