#!/usr/bin/env node
// Read-only: verify member 3's transactions and current order/balance observations.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const {execFileSync} = require('node:child_process');
const {Interface, formatUnits} = require(process.env.ETHERS_MODULE || 'ethers');
const root = path.resolve(__dirname, '..');
const dir = path.join(root, 'evidence/potos/records');
const input = JSON.parse(fs.readFileSync(path.join(dir, 'buyer-reported-transactions.json')));
const abi = new Interface(JSON.parse(fs.readFileSync(path.join(root, 'build/resources/main/solidity/Escrow.abi'))));
const tokenAbi = new Interface(JSON.parse(fs.readFileSync(path.join(root, 'build/resources/main/solidity/WQLToken.abi'))));
const same = (a,b) => assert.equal(a.toLowerCase(), b.toLowerCase());
const wanted = JSON.parse(fs.readFileSync(path.join(dir, 'arbitration-reported-transactions.json'))).transactions;
function rpc(method, params) {
  const r = JSON.parse(execFileSync('curl', ['--fail-with-body','--silent','--show-error','--max-time','30',
    '--tlsv1.2','--tls-max','1.2','--retry','2','--retry-all-errors',input.rpc,'-H','Content-Type: application/json','--data-binary','@-'],
    {input:JSON.stringify({jsonrpc:'2.0',id:1,method,params}),encoding:'utf8'}));
  if(r.error)throw Error(JSON.stringify(r.error));
  return r.result;
}
assert.equal(Number(BigInt(rpc('eth_chainId',[]))),60600);
const raw = {};
const transactions = wanted.map(w => {
  const receipt=rpc('eth_getTransactionReceipt',[w.hash]);
  const tx=rpc('eth_getTransactionByHash',[w.hash]);
  assert(receipt && tx);
  for(const v of [receipt,tx]) {same(v.from,input.arbiter);same(v.to,input.escrow);}
  same(receipt.transactionHash,w.hash);same(tx.hash,w.hash);
  assert.equal(receipt.status,w.status);assert.equal(Number(BigInt(receipt.blockNumber)),w.block);
  assert.equal(Number(BigInt(receipt.gasUsed)),w.gas);
  const parsed=abi.parseTransaction({data:tx.input,value:tx.value});
  assert.equal(parsed.name,'resolve');assert.equal(parsed.args[0],BigInt(w.id));assert.equal(parsed.args[1],w.sellerWins);assert.equal(BigInt(tx.value),0n);
  const events=receipt.logs.map(log => {
    const iface=log.address.toLowerCase()===input.token.toLowerCase()?tokenAbi:abi;
    assert([input.token.toLowerCase(),input.escrow.toLowerCase()].includes(log.address.toLowerCase()));
    const e=iface.parseLog(log);assert(e);
    return {address:log.address,name:e.name,args:Object.fromEntries(e.fragment.inputs.map((arg,i)=>[arg.name,typeof e.args[i]==='bigint'?e.args[i].toString():e.args[i]]))};
  });
  if(w.id===0) assert.equal(events.length,0);
  else {
    const transfer=events.find(e=>e.name==='Transfer');assert(transfer);
    same(transfer.args.from,input.escrow);same(transfer.args.to,input.buyer);assert.equal(transfer.args.value,'30000000000000000000');
    const resolved=events.find(e=>e.name==='OrderResolved');assert(resolved);
    assert.equal(resolved.args.id,'1');assert.equal(resolved.args.sellerWins,false);
  }
  const block=rpc('eth_getBlockByNumber',[receipt.blockNumber,false]);
  const price=BigInt(receipt.effectiveGasPrice);assert.equal(price,0n);
  raw[w.label]={transaction:tx,receipt};
  return {label:w.label,transactionHash:w.hash,from:tx.from,to:tx.to,method:parsed.name,orderId:w.id,sellerWins:w.sellerWins,
    blockNumber:w.block,blockTime:new Date(Number(BigInt(block.timestamp))*1000).toISOString(),status:receipt.status,
    gasUsed:BigInt(receipt.gasUsed).toString(),effectiveGasPrice:price.toString(),executionFeeWei:(BigInt(receipt.gasUsed)*price).toString(),executionFeePOT:formatUnits(BigInt(receipt.gasUsed)*price,18),events};
});
function snapshot(blockNumber) {
  const tag='0x'+BigInt(blockNumber).toString(16);
  function call(address,iface,name,args){return iface.decodeFunctionResult(name,rpc('eth_call',[{to:address,data:iface.encodeFunctionData(name,args)},tag]));}
  same(call(input.escrow,abi,'arbiter',[])[0],input.arbiter);
  const orders=[0,1].map(id=>{
    const o=call(input.escrow,abi,'getOrder',[id])[0];
    same(o.buyer,input.buyer);same(o.seller,input.seller);same(o.token,input.token);assert.equal(o.amount,30n*10n**18n);
    return {id,buyer:o.buyer,seller:o.seller,token:o.token,amount:o.amount.toString(),deadline:o.deadline.toString(),statusCode:Number(o.status),status:['Created','Paid','Completed','Refunded','Disputed'][Number(o.status)],description:o.description};
  });
  const tokenBalances=Object.fromEntries(['buyer','seller','escrow'].map(role=>[role,formatUnits(call(input.token,tokenAbi,'balanceOf',[input[role]])[0],18)]));
  return {requestedBlock:blockNumber,orders,tokenBalances};
}
// This endpoint returned Refunded even at pre-refund block 2823578. Do not
// misrepresent a historical blockTag request as an independently proven snapshot.
const historicalProbe=snapshot(2823578);
const current=snapshot(Number(BigInt(rpc('eth_blockNumber',[]))));
current.finishedAtBlock=Number(BigInt(rpc('eth_blockNumber',[])));
assert.deepEqual(current.orders.map(o=>o.status),['Paid','Refunded']);
assert.deepEqual(current.tokenBalances,{buyer:'40.0',seller:'30.0',escrow:'30.0'});
const report={classification:'POTOS_TESTNET_ARBITRATION_INDEPENDENTLY_VERIFIED',verifiedAt:new Date().toISOString(),rpc:input.rpc,chainId:60600,escrow:input.escrow,arbiter:input.arbiter,
  operatorAttribution:'User reports member 3 Yuan Jiajun personally sent the HTTP requests; chain verifies BE signer, not human operator.',
  httpEvidence:'HTTP 409/200 and response text are shown in user-supplied terminal record screenshots; original HTTP response files have not been supplied.',
  eventNamingNote:'Project ABI defines OrderResolved(uint256 indexed id, bool sellerWins). Explorer labels refundToBuyer differently; names are not encoded in topics/data. False here is sellerWins=false, confirmed by Transfer to buyer and Refunded state.',
  historicalStateLimitation:'RPC returned Refunded at requested pre-refund block 2823578. Historical blockTag behavior is unreliable; current readings are bounded by requestedBlock and finishedAtBlock, not asserted as atomic historical snapshots.',
  member3ReportedSnapshot:{blockNumber:2823657,orders:['Paid','Refunded'],tokenBalances:{buyer:'40',seller:'30',escrow:'30'},source:'User report, not independently established as a historical snapshot'},
  transactions,historicalProbe,current};
fs.writeFileSync(path.join(dir,'arbitration-verified-receipts.json'),JSON.stringify(Object.fromEntries(Object.entries(raw).map(([k,v])=>[k,v.receipt])),null,2)+'\n');
fs.writeFileSync(path.join(dir,'arbitration-verified-transactions.json'),JSON.stringify(Object.fromEntries(Object.entries(raw).map(([k,v])=>[k,Object.fromEntries(['hash','blockHash','blockNumber','from','to','input','value','gas','gasPrice','nonce','chainId'].filter(field=>field in v.transaction).map(field=>[field,v.transaction[field]]))])),null,2)+'\n');
fs.writeFileSync(path.join(dir,'arbitration-verification.json'),JSON.stringify(report,null,2)+'\n');
console.log(JSON.stringify({transactions,current,historicalStateLimitation:report.historicalStateLimitation},null,2));
