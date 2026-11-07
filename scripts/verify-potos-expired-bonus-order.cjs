#!/usr/bin/env node
// Read-only evidence for the first bonus order's expired deposit. Never signs or sends transactions.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const {execFileSync} = require('node:child_process');
const {Interface, formatUnits} = require(process.env.ETHERS_MODULE || 'ethers');
const root = path.resolve(__dirname, '..');
const dir = path.join(root, 'evidence/bonus/potos');
const deployment = JSON.parse(fs.readFileSync(path.join(dir, 'deployment-verification.json')));
const escrow = deployment.contractAddress;
const buyer = deployment.secondArbiter;
const seller = '0xE0F0970f35FFE50dB6a6AD25ba775674D24682D2';
const tokenAddress = deployment.current.token;
const contract = new Interface(JSON.parse(fs.readFileSync(path.join(root, 'build/resources/main/solidity/EscrowEnhanced.abi'))));
const token = new Interface(['function allowance(address,address) view returns (uint256)',
  'function balanceOf(address) view returns (uint256)']);
const hashes = {create:process.argv[2],deposit:process.argv[3]};
for (const hash of Object.values(hashes)) {
  assert(/^0x[0-9a-fA-F]{64}$/.test(hash), 'Pass the public create and failed deposit transaction hashes');
}
const same = (a,b) => assert.equal(a.toLowerCase(), b.toLowerCase());
function rpc(method, params) {
  const response = JSON.parse(execFileSync('curl', ['--fail-with-body','--silent','--show-error',
    '--max-time','30','--tlsv1.2','--tls-max','1.2','--retry','2','--retry-all-errors',
    deployment.rpc,'-H','Content-Type: application/json','--data-binary','@-'],
    {input:JSON.stringify({jsonrpc:'2.0',id:1,method,params}),encoding:'utf8'}));
  if (response.error) throw Error(JSON.stringify(response.error));
  assert('result' in response, 'RPC response has no result');
  return response.result;
}
function call(address, iface, method, args=[]) {
  return iface.decodeFunctionResult(method, rpc('eth_call', [{to:address,data:iface.encodeFunctionData(method,args)},'latest']));
}
const beijing = seconds => new Date(Number(seconds)*1000).toLocaleString('sv-SE', {timeZone:'Asia/Shanghai'})+' +08:00';
assert.equal(BigInt(rpc('eth_chainId', [])), 60600n);
const receipts = {}, transactions = {}, blocks = {};
for (const [name,hash] of Object.entries(hashes)) {
  const receipt = rpc('eth_getTransactionReceipt', [hash]);
  const tx = rpc('eth_getTransactionByHash', [hash]);
  assert(receipt && tx);
  same(receipt.transactionHash,hash);same(tx.hash,hash);
  for (const item of [receipt,tx]) {same(item.from,buyer);same(item.to,escrow);}
  assert.equal(BigInt(tx.value),0n);assert.equal(BigInt(tx.chainId),60600n);
  receipts[name] = receipt;
  transactions[name] = Object.fromEntries(['hash','blockHash','blockNumber','from','to','input','value','gas','gasPrice','nonce','chainId']
    .filter(k=>k in tx).map(k=>[k,tx[k]]));
  blocks[name] = rpc('eth_getBlockByNumber', [receipt.blockNumber,false]);
}
assert.equal(receipts.create.status,'0x1');assert.equal(receipts.deposit.status,'0x0');
const creation = contract.parseTransaction({data:transactions.create.input,value:0});
const deposit = contract.parseTransaction({data:transactions.deposit.input,value:0});
assert.equal(creation.name,'createOrder');same(creation.args[0],seller);same(creation.args[1],tokenAddress);
assert.equal(creation.args[2],1000000000000000000n);assert.equal(creation.args[4],'bonus shipped confirm');
assert.equal(deposit.name,'deposit');assert.equal(deposit.args[0],0n);
const events = receipts.create.logs.filter(log=>log.address.toLowerCase()===escrow.toLowerCase())
  .map(log=>contract.parseLog(log)).filter(event=>event && event.name==='OrderCreated');
assert.equal(events.length,1);assert.equal(events[0].args.id,0n);assert.equal(receipts.deposit.logs.length,0);
const startBlock = Number(BigInt(rpc('eth_blockNumber',[])));
const order = call(escrow,contract,'getOrder',[0])[0];
same(order.buyer,buyer);same(order.seller,seller);same(order.token,tokenAddress);
assert.equal(order.amount,1000000000000000000n);assert.equal(order.deadline,creation.args[3]);
assert.equal(order.status,0n);assert(BigInt(blocks.deposit.timestamp)>order.deadline);
const orderCount = call(escrow,contract,'orderCount')[0];
const allowance = call(tokenAddress,token,'allowance',[buyer,escrow])[0];
const balances = Object.fromEntries(Object.entries({buyer,seller,escrow})
  .map(([role,address])=>[role,formatUnits(call(tokenAddress,token,'balanceOf',[address])[0],18)]));
const finish = rpc('eth_getBlockByNumber',['latest',false]);
const report = {
  classification:'REAL_POTOS_EXPIRED_ORDER_AND_FAILED_DEPOSIT_INDEPENDENTLY_VERIFIED',
  verifiedAt:new Date().toISOString(),chainId:60600,rpc:deployment.rpc,contractAddress:escrow,
  create:{hash:hashes.create,status:receipts.create.status,block:Number(BigInt(receipts.create.blockNumber)),
    timeBeijing:beijing(BigInt(blocks.create.timestamp)),deadline:order.deadline.toString(),
    deadlineBeijing:beijing(order.deadline),secondsRemainingWhenCreated:Number(order.deadline-BigInt(blocks.create.timestamp))},
  deposit:{hash:hashes.deposit,status:receipts.deposit.status,block:Number(BigInt(receipts.deposit.blockNumber)),
    timeBeijing:beijing(BigInt(blocks.deposit.timestamp)),secondsAfterDeadline:Number(BigInt(blocks.deposit.timestamp)-order.deadline),
    gasUsed:BigInt(receipts.deposit.gasUsed).toString(),effectiveGasPrice:BigInt(receipts.deposit.effectiveGasPrice).toString(),
    executionFeePOT:formatUnits(BigInt(receipts.deposit.gasUsed)*BigInt(receipts.deposit.effectiveGasPrice),18),eventCount:receipts.deposit.logs.length},
  current:{startBlock,finishBlock:Number(BigInt(finish.number)),chainTimeBeijing:beijing(BigInt(finish.timestamp)),
    orderCount:orderCount.toString(),order0:{buyer,seller,token:tokenAddress,amount:order.amount.toString(),
      status:Number(order.status),statusName:'Created',expired:BigInt(finish.timestamp)>order.deadline,description:order.description},
    allowance:allowance.toString(),balances,readSemantics:'Sequential latest reads, not an atomic historical snapshot'},
  diagnosis:'The deposit block timestamp exceeded the immutable deadline; the transaction reverted with no token transfers. Preserve order 0 and never retry its deposit.',
  browserOriginalsReceivedLocally:false
};
fs.writeFileSync(path.join(dir,'expired-order0-verified-receipts.json'),JSON.stringify(receipts,null,2)+'\n');
fs.writeFileSync(path.join(dir,'expired-order0-verified-transactions.json'),JSON.stringify(transactions,null,2)+'\n');
fs.writeFileSync(path.join(dir,'expired-order0-verification.json'),JSON.stringify(report,null,2)+'\n');
console.log(JSON.stringify(report,null,2));
