#!/usr/bin/env node
// Read-only: independently verify a seller dispute and snapshot both orders.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const {execFileSync} = require('node:child_process');
const {Interface, formatUnits} = require(process.env.ETHERS_MODULE || 'ethers');
const root = path.resolve(__dirname, '..');
const dir = path.join(root, 'evidence/potos/records');
const input = JSON.parse(fs.readFileSync(path.join(dir, 'buyer-reported-transactions.json')));
const hash = process.argv[2];
assert(/^0x[0-9a-fA-F]{64}$/.test(hash), 'Pass the public dispute transaction hash');
const abi = new Interface(JSON.parse(fs.readFileSync(path.join(root, 'build/resources/main/solidity/Escrow.abi'))));
const tokenAbi = new Interface(JSON.parse(fs.readFileSync(path.join(root, 'build/resources/main/solidity/WQLToken.abi'))));
const same = (a,b) => assert.equal(a.toLowerCase(), b.toLowerCase());
function rpc(method, params) {
  const r = JSON.parse(execFileSync('curl', ['--fail-with-body','--silent','--show-error','--max-time','30',
    '--tlsv1.2','--tls-max','1.2','--retry','2','--retry-all-errors',input.rpc,'-H','Content-Type: application/json','--data-binary','@-'],
    {input:JSON.stringify({jsonrpc:'2.0',id:1,method,params}),encoding:'utf8'}));
  if(r.error)throw Error(JSON.stringify(r.error));
  return r.result;
}
assert.equal(Number(BigInt(rpc('eth_chainId',[]))),60600);
const receipt=rpc('eth_getTransactionReceipt',[hash]);
const tx=rpc('eth_getTransactionByHash',[hash]);
assert(receipt && tx); assert.equal(receipt.status,'0x1');
same(receipt.transactionHash,hash);same(tx.hash,hash);
same(receipt.from,input.seller);same(tx.from,input.seller);
same(receipt.to,input.escrow);same(tx.to,input.escrow);
const parsed=abi.parseTransaction({data:tx.input,value:tx.value});
assert.equal(parsed.name,'dispute');assert.equal(parsed.args[0],1n);assert.equal(BigInt(tx.value),0n);
const events=receipt.logs.map(log=>{
  same(log.address,input.escrow);
  const e=abi.parseLog(log);
  return {address:log.address,name:e.name,args:Object.fromEntries(e.fragment.inputs.map((arg,i)=>[arg.name,typeof e.args[i]==='bigint'?e.args[i].toString():e.args[i]]))};
});
const event=events.find(e=>e.name==='OrderDisputed');assert(event);assert.equal(event.args.id,'1');same(event.args.by,input.seller);
const block=rpc('eth_getBlockByNumber',[receipt.blockNumber,false]);
const tag=rpc('eth_blockNumber',[]);
function call(address,iface,name,args){return iface.decodeFunctionResult(name,rpc('eth_call',[{to:address,data:iface.encodeFunctionData(name,args)},tag]));}
const arbiter=call(input.escrow,abi,'arbiter',[])[0];same(arbiter,input.arbiter);
const orders=[0,1].map(id=>{
  const o=call(input.escrow,abi,'getOrder',[id])[0];
  same(o.buyer,input.buyer);same(o.seller,input.seller);same(o.token,input.token);
  assert.equal(o.amount,30n*10n**18n);
  return {id,buyer:o.buyer,seller:o.seller,token:o.token,amount:o.amount.toString(),deadline:o.deadline.toString(),statusCode:Number(o.status),status:['Created','Paid','Completed','Refunded','Disputed'][Number(o.status)],description:o.description};
});
assert.equal(orders[0].status,'Paid');assert.equal(orders[1].status,'Disputed');
const balances=Object.fromEntries(['buyer','seller','escrow'].map(role=>[role,formatUnits(call(input.token,tokenAbi,'balanceOf',[input[role]])[0],18)]));
const report={classification:'POTOS_TESTNET_SELLER_DISPUTE_INDEPENDENTLY_VERIFIED',verifiedAt:new Date().toISOString(),rpc:input.rpc,chainId:60600,escrow:input.escrow,arbiter,observedBlock:Number(BigInt(tag)),
  transactionHash:hash,from:tx.from,to:tx.to,method:parsed.name,orderId:1,blockNumber:Number(BigInt(receipt.blockNumber)),blockTime:new Date(Number(BigInt(block.timestamp))*1000).toISOString(),receiptStatus:receipt.status,gasUsed:BigInt(receipt.gasUsed).toString(),events,orders,tokenBalances:balances};
fs.writeFileSync(path.join(dir,'seller-verified-receipts.json'),JSON.stringify({dispute1:receipt},null,2)+'\n');
fs.writeFileSync(path.join(dir,'seller-dispute-verification.json'),JSON.stringify(report,null,2)+'\n');
console.log(JSON.stringify(report,null,2));
