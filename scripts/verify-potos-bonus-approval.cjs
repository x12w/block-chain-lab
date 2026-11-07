#!/usr/bin/env node
// Read-only verification of the buyer's finite allowance for the enhanced contract.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const {execFileSync} = require('node:child_process');
const {Interface, formatUnits} = require(process.env.ETHERS_MODULE || 'ethers');
const root = path.resolve(__dirname, '..');
const dir = path.join(root, 'evidence/bonus/potos');
const deployed = JSON.parse(fs.readFileSync(path.join(dir, 'deployment-verification.json')));
const hash = process.argv[2];
assert(/^0x[0-9a-fA-F]{64}$/.test(hash), 'Pass the public approval transaction hash');
const buyer = deployed.secondArbiter;
const seller = '0xE0F0970f35FFE50dB6a6AD25ba775674D24682D2';
const escrow = deployed.contractAddress;
const tokenAddress = deployed.current.token;
const token = new Interface(['function approve(address,uint256) returns (bool)',
  'function decimals() view returns (uint8)', 'function balanceOf(address) view returns (uint256)',
  'function allowance(address,address) view returns (uint256)',
  'event Approval(address indexed owner,address indexed spender,uint256 value)']);
const contract = new Interface(['function version() view returns (uint256)',
  'function arbiter() view returns (address)', 'function secondArbiter() view returns (address)',
  'function orderCount() view returns (uint256)']);
const same = (a,b) => assert.equal(a.toLowerCase(),b.toLowerCase());
function rpc(method, params) {
  const response = JSON.parse(execFileSync('curl',['--fail-with-body','--silent','--show-error',
    '--max-time','30','--tlsv1.2','--tls-max','1.2','--retry','2','--retry-all-errors',
    deployed.rpc,'-H','Content-Type: application/json','--data-binary','@-'],
    {input:JSON.stringify({jsonrpc:'2.0',id:1,method,params}),encoding:'utf8'}));
  if (response.error) throw Error(JSON.stringify(response.error));
  return response.result;
}
function call(address, iface, method, args=[]) {
  return iface.decodeFunctionResult(method,rpc('eth_call',[{to:address,data:iface.encodeFunctionData(method,args)},'latest']));
}
assert.equal(BigInt(rpc('eth_chainId',[])),60600n);
const receipt = rpc('eth_getTransactionReceipt',[hash]);
const tx = rpc('eth_getTransactionByHash',[hash]);
assert(receipt && tx);assert.equal(receipt.status,'0x1');
same(receipt.transactionHash,hash);same(tx.hash,hash);
for (const item of [receipt,tx]) {same(item.from,buyer);same(item.to,tokenAddress);}
assert.equal(BigInt(tx.value),0n);assert.equal(BigInt(tx.chainId),60600n);
const decoded = token.parseTransaction({data:tx.input,value:tx.value});
assert.equal(decoded.name,'approve');same(decoded.args[0],escrow);
assert.equal(decoded.args[1],3000000000000000000n);
const approvals = receipt.logs.filter(log=>log.address.toLowerCase()===tokenAddress.toLowerCase())
  .map(log=>token.parseLog(log)).filter(event=>event && event.name==='Approval');
assert.equal(approvals.length,1);
same(approvals[0].args.owner,buyer);same(approvals[0].args.spender,escrow);
assert.equal(approvals[0].args.value,3000000000000000000n);
const block = rpc('eth_getBlockByNumber',[receipt.blockNumber,false]);
const startBlock = Number(BigInt(rpc('eth_blockNumber',[])));
assert.equal(call(escrow,contract,'version')[0],2n);
same(call(escrow,contract,'arbiter')[0],deployed.firstArbiter);
same(call(escrow,contract,'secondArbiter')[0],buyer);
const decimals = Number(call(tokenAddress,token,'decimals')[0]);assert.equal(decimals,18);
const allowance = call(tokenAddress,token,'allowance',[buyer,escrow])[0];
const orderCount = call(escrow,contract,'orderCount')[0];
const balances = Object.fromEntries(Object.entries({buyer,seller,escrow})
  .map(([role,address])=>[role,formatUnits(call(tokenAddress,token,'balanceOf',[address])[0],18)]));
const finishBlock = Number(BigInt(rpc('eth_blockNumber',[])));
const report = {classification:'REAL_POTOS_BONUS_APPROVAL_INDEPENDENTLY_VERIFIED',
  verifiedAt:new Date().toISOString(),chainId:60600,rpc:deployed.rpc,
  transactionHash:hash,blockNumber:Number(BigInt(receipt.blockNumber)),
  transactionTime:new Date(Number(BigInt(block.timestamp))*1000).toISOString(),
  status:receipt.status,from:buyer,to:tokenAddress,method:'approve',spender:escrow,
  approvedAmount:'3000000000000000000',approvedTokens:'3',gasUsed:BigInt(receipt.gasUsed).toString(),
  effectiveGasPrice:BigInt(receipt.effectiveGasPrice).toString(),
  executionFeePOT:formatUnits(BigInt(receipt.gasUsed)*BigInt(receipt.effectiveGasPrice),18),
  approvalEvent:{owner:buyer,spender:escrow,value:'3000000000000000000'},
  current:{startBlock,finishBlock,allowance:allowance.toString(),orderCount:orderCount.toString(),balances,
    readyForOrderCreation:allowance===3000000000000000000n && orderCount===0n,
    readSemantics:'Sequential latest reads, not an atomic historical snapshot'},
  browserEvidence:{thread:'确认 Remix 界面',threadId:'6aee7b95-237c-83e8-817b-09e63e643ddc',
    reportedArchive:'route_a_bonus_approval.zip',reportedScreenshots:9,localOriginalArchiveReceived:false},
  remaining:'Wait for seller readiness, then create and fund new orders; shipping and arbitration are not yet accepted'};
fs.writeFileSync(path.join(dir,'approval-verified-receipts.json'),JSON.stringify({approval:receipt},null,2)+'\n');
fs.writeFileSync(path.join(dir,'approval-verified-transaction.json'),JSON.stringify(Object.fromEntries(
  ['hash','blockHash','blockNumber','from','to','input','value','gas','gasPrice','nonce','chainId'].filter(k=>k in tx).map(k=>[k,tx[k]])),null,2)+'\n');
fs.writeFileSync(path.join(dir,'approval-verification.json'),JSON.stringify(report,null,2)+'\n');
console.log(JSON.stringify(report,null,2));
