#!/usr/bin/env node
// Read-only: verify the final buyer confirmation and observe current settlement state.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const {execFileSync} = require('node:child_process');
const {Interface, formatUnits} = require(process.env.ETHERS_MODULE || 'ethers');
const root = path.resolve(__dirname, '..');
const dir = path.join(root, 'evidence/potos/records');
const input = JSON.parse(fs.readFileSync(path.join(dir, 'buyer-reported-transactions.json')));
const hash = process.argv[2];
assert(/^0x[0-9a-fA-F]{64}$/.test(hash), 'Pass the public confirm(0) transaction hash');
const abi = new Interface(JSON.parse(fs.readFileSync(path.join(root, 'build/resources/main/solidity/Escrow.abi'))));
const tokenAbi = new Interface(JSON.parse(fs.readFileSync(path.join(root, 'build/resources/main/solidity/WQLToken.abi'))));
const same = (a,b) => assert.equal(a.toLowerCase(), b.toLowerCase());
function rpc(method, params) {
  const response = JSON.parse(execFileSync('curl', ['--fail-with-body','--silent','--show-error','--max-time','30',
    '--tlsv1.2','--tls-max','1.2','--retry','2','--retry-all-errors',input.rpc,'-H','Content-Type: application/json','--data-binary','@-'],
    {input:JSON.stringify({jsonrpc:'2.0',id:1,method,params}),encoding:'utf8'}));
  if(response.error)throw Error(JSON.stringify(response.error));
  return response.result;
}
assert.equal(Number(BigInt(rpc('eth_chainId',[]))),60600);
const receipt=rpc('eth_getTransactionReceipt',[hash]);
const tx=rpc('eth_getTransactionByHash',[hash]);
assert(receipt && tx);assert.equal(receipt.status,'0x1');
same(receipt.transactionHash,hash);same(tx.hash,hash);
for(const value of [receipt,tx]) {same(value.from,input.buyer);same(value.to,input.escrow);}
const parsed=abi.parseTransaction({data:tx.input,value:tx.value});
assert.equal(parsed.name,'confirm');assert.equal(parsed.args[0],0n);assert.equal(BigInt(tx.value),0n);
const events=receipt.logs.map(log=>{
  assert([input.token.toLowerCase(),input.escrow.toLowerCase()].includes(log.address.toLowerCase()));
  const e=(log.address.toLowerCase()===input.token.toLowerCase()?tokenAbi:abi).parseLog(log);assert(e);
  return {address:log.address,name:e.name,args:Object.fromEntries(e.fragment.inputs.map((arg,i)=>[arg.name,typeof e.args[i]==='bigint'?e.args[i].toString():e.args[i]]))};
});
const transfer=events.find(e=>e.name==='Transfer');assert(transfer);
same(transfer.address,input.token);same(transfer.args.from,input.escrow);same(transfer.args.to,input.seller);
assert.equal(transfer.args.value,'30000000000000000000');
const completed=events.find(e=>e.name==='OrderCompleted');assert(completed);
same(completed.address,input.escrow);assert.equal(completed.args.id,'0');
const block=rpc('eth_getBlockByNumber',[receipt.blockNumber,false]);
const startBlock=Number(BigInt(rpc('eth_blockNumber',[])));
function call(address,iface,name,args){return iface.decodeFunctionResult(name,rpc('eth_call',[{to:address,data:iface.encodeFunctionData(name,args)},'latest']));}
same(call(input.escrow,abi,'arbiter',[])[0],input.arbiter);
const orders=[0,1].map(id=>{
  const o=call(input.escrow,abi,'getOrder',[id])[0];
  same(o.buyer,input.buyer);same(o.seller,input.seller);same(o.token,input.token);assert.equal(o.amount,30n*10n**18n);
  return {id,buyer:o.buyer,seller:o.seller,token:o.token,amount:o.amount.toString(),deadline:o.deadline.toString(),statusCode:Number(o.status),status:['Created','Paid','Completed','Refunded','Disputed'][Number(o.status)],description:o.description};
});
const tokenBalances=Object.fromEntries(['buyer','seller','escrow'].map(role=>[role,formatUnits(call(input.token,tokenAbi,'balanceOf',[input[role]])[0],18)]));
const allowance=call(input.token,tokenAbi,'allowance',[input.buyer,input.escrow])[0].toString();
const finishBlock=Number(BigInt(rpc('eth_blockNumber',[])));
assert.deepEqual(orders.map(o=>o.status),['Completed','Refunded']);
assert.deepEqual(tokenBalances,{buyer:'40.0',seller:'60.0',escrow:'0.0'});
const price=BigInt(receipt.effectiveGasPrice);
const report={classification:'POTOS_TESTNET_BUYER_FINAL_CONFIRMATION_INDEPENDENTLY_VERIFIED',verifiedAt:new Date().toISOString(),rpc:input.rpc,chainId:60600,escrow:input.escrow,
  transactionHash:hash,from:tx.from,to:tx.to,method:'confirm',orderId:0,blockNumber:Number(BigInt(receipt.blockNumber)),blockTime:new Date(Number(BigInt(block.timestamp))*1000).toISOString(),status:receipt.status,
  gasUsed:BigInt(receipt.gasUsed).toString(),effectiveGasPrice:price.toString(),executionFeeWei:(BigInt(receipt.gasUsed)*price).toString(),executionFeePOT:formatUnits(BigInt(receipt.gasUsed)*price,18),events,
  current:{startBlock,finishBlock,orders,tokenBalances,buyerAllowance:allowance,readSemantics:'Sequential latest reads during the stated block range; not an atomic historical snapshot. Historical blockTag behavior of this RPC was previously found unreliable.'}};
fs.writeFileSync(path.join(dir,'final-confirm-verified-receipts.json'),JSON.stringify({confirm0:receipt},null,2)+'\n');
fs.writeFileSync(path.join(dir,'final-confirm-verified-transactions.json'),JSON.stringify({confirm0:Object.fromEntries(['hash','blockHash','blockNumber','from','to','input','value','gas','gasPrice','nonce','chainId'].filter(field=>field in tx).map(field=>[field,tx[field]]))},null,2)+'\n');
fs.writeFileSync(path.join(dir,'final-confirm-verification.json'),JSON.stringify(report,null,2)+'\n');
console.log(JSON.stringify(report,null,2));
