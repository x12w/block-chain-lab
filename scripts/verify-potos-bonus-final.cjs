#!/usr/bin/env node
// Read-only final receipt, event and settlement verification. No signing or transaction submission.
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const {execFileSync}=require('node:child_process');
const {Interface,formatUnits}=require(process.env.ETHERS_MODULE || 'ethers');
const root=path.resolve(__dirname,'..'),dir=path.join(root,'evidence/bonus/potos');
const input=JSON.parse(fs.readFileSync(path.join(dir,'final-reported-transactions.json')));
const deploy=JSON.parse(fs.readFileSync(path.join(dir,'deployment-verification.json')));
const contract=new Interface(JSON.parse(fs.readFileSync(path.join(root,'build/resources/main/solidity/EscrowEnhanced.abi'))));
const token=new Interface(['function balanceOf(address) view returns(uint256)','function allowance(address,address) view returns(uint256)',
  'event Transfer(address indexed from,address indexed to,uint256 value)']);
const escrow=deploy.contractAddress,buyer=deploy.secondArbiter,seller='0xE0F0970f35FFE50dB6a6AD25ba775674D24682D2',tokenAddress=deploy.current.token;
const same=(a,b)=>assert.equal(a.toLowerCase(),b.toLowerCase());
const beijing=timestamp=>new Date(Number(timestamp)*1000).toLocaleString('sv-SE',{timeZone:'Asia/Shanghai'});
function batch(requests){
  assert(requests.length>0&&requests.length<=8);
  const response=JSON.parse(execFileSync('curl',['--fail-with-body','--silent','--show-error','--max-time','30',
    '--tlsv1.2','--tls-max','1.2','--retry','2','--retry-all-errors',deploy.rpc,'-H','Content-Type: application/json','--data-binary','@-'],
    {input:JSON.stringify(requests.map(([method,params],id)=>({jsonrpc:'2.0',id,method,params}))),encoding:'utf8'}));
  if(!Array.isArray(response))throw Error(JSON.stringify(response));
  const byId=new Map(response.map(x=>[x.id,x]));assert.equal(byId.size,requests.length);
  return requests.map((_,id)=>{const r=byId.get(id);assert(r);if(r.error)throw Error(JSON.stringify(r.error));assert('result' in r);return r.result;});
}
const callRequest=(address,iface,method,args=[])=>['eth_call',[{to:address,data:iface.encodeFunctionData(method,args)},'latest']];
const transactions={},receipts={},blocks={},summaries=[];
const names=Object.keys(input).filter(k=>k!=='source');
const values=batch(names.flatMap(k=>[['eth_getTransactionReceipt',[input[k].hash]],['eth_getTransactionByHash',[input[k].hash]]]));
const blockValues=batch(names.map((_,i)=>['eth_getBlockByNumber',[values[i*2].blockNumber,false]]));
for(let i=0;i<names.length;i++){
  const name=names[i],expected=input[name],receipt=values[i*2],tx=values[i*2+1],block=blockValues[i];
  assert(receipt&&tx);same(receipt.transactionHash,expected.hash);same(tx.hash,expected.hash);assert.equal(receipt.status,'0x1');
  same(tx.from,buyer);same(tx.to,escrow);same(receipt.from,buyer);same(receipt.to,escrow);assert.equal(BigInt(tx.value),0n);assert.equal(BigInt(tx.chainId),60600n);
  const parsed=contract.parseTransaction({data:tx.input,value:tx.value});assert.equal(parsed.name,expected.method);
  assert.deepEqual(Array.from(parsed.args).map(v=>typeof v==='bigint'?Number(v):v),expected.args);
  const events=receipt.logs.map(log=>{const isToken=log.address.toLowerCase()===tokenAddress.toLowerCase();
    assert(isToken||log.address.toLowerCase()===escrow.toLowerCase());const e=(isToken?token:contract).parseLog(log);assert(e);
    return {name:e.name,address:log.address,args:Object.fromEntries(e.fragment.inputs.map((arg,j)=>[arg.name,typeof e.args[j]==='bigint'?e.args[j].toString():e.args[j]]))};});
  const expectedEvents={conflictingVote:['ArbitrationVoted'],agreedRefund:['ArbitrationVoted','Transfer','OrderResolved'],timeoutRefund:['Transfer','OrderRefunded'],shippedConfirm:['Transfer','OrderCompleted']}[name];
  assert.deepEqual(events.map(e=>e.name),expectedEvents);
  for(const e of events){if(e.name==='Transfer'){same(e.args.from,escrow);same(e.args.to,name==='shippedConfirm'?seller:buyer);assert.equal(e.args.value,'1000000000000000000');}
    else {assert.equal(e.args.id,String(expected.args[0]));if(e.name==='ArbitrationVoted'){same(e.args.voter,buyer);assert.equal(e.args.sellerWins,expected.args[1]);}if(e.name==='OrderResolved')assert.equal(e.args.sellerWins,false);}}
  receipts[name]=receipt;transactions[name]=Object.fromEntries(['hash','blockHash','blockNumber','from','to','input','value','gas','gasPrice','nonce','chainId'].filter(k=>k in tx).map(k=>[k,tx[k]]));
  blocks[name]=Object.fromEntries(['number','hash','timestamp'].map(k=>[k,block[k]]));
  summaries.push({stage:name,hash:expected.hash,method:expected.method,args:expected.args,status:receipt.status,blockNumber:Number(BigInt(receipt.blockNumber)),timeBeijing:beijing(BigInt(block.timestamp)),gasUsed:BigInt(receipt.gasUsed).toString(),effectiveGasPrice:BigInt(receipt.effectiveGasPrice).toString(),executionFeePOT:formatUnits(BigInt(receipt.gasUsed)*BigInt(receipt.effectiveGasPrice),18),events});
}
const first=JSON.parse(fs.readFileSync(path.join(dir,'first-vote-verification.json')));
assert(first.blockNumber<summaries[0].blockNumber);for(let i=1;i<summaries.length;i++)assert(summaries[i-1].blockNumber<summaries[i].blockNumber);
const start=batch([['eth_chainId',[]],['eth_blockNumber',[]]]) ;assert.equal(BigInt(start[0]),60600n);
const rawOrders=batch([0,1,2,3].map(id=>callRequest(escrow,contract,'getOrder',[id])));
const orders=rawOrders.map((raw,id)=>{const o=contract.decodeFunctionResult('getOrder',raw)[0];same(o.buyer,buyer);same(o.seller,seller);same(o.token,tokenAddress);assert.equal(o.amount,1000000000000000000n);
  return {id,buyer:o.buyer,seller:o.seller,token:o.token,amount:o.amount.toString(),deadline:o.deadline.toString(),deadlineBeijing:beijing(o.deadline),statusCode:Number(o.status),status:['Created','Paid','Completed','Refunded','Disputed','Shipped'][Number(o.status)],description:o.description};});
const rawCurrent=batch([callRequest(escrow,contract,'getVotes',[2]),callRequest(tokenAddress,token,'allowance',[buyer,escrow]),
  ...[buyer,seller,escrow].map(address=>callRequest(tokenAddress,token,'balanceOf',[address])),callRequest(escrow,contract,'orderCount'),['eth_getBlockByNumber',['latest',false]]]);
const votes=Array.from(contract.decodeFunctionResult('getVotes',rawCurrent[0]));
const allowance=token.decodeFunctionResult('allowance',rawCurrent[1])[0].toString();
const balances=Object.fromEntries(['buyer','seller','escrow'].map((role,i)=>[role,formatUnits(token.decodeFunctionResult('balanceOf',rawCurrent[2+i])[0],18)]));
const count=contract.decodeFunctionResult('orderCount',rawCurrent[5])[0].toString(),finish=rawCurrent[6];
assert.deepEqual(orders.map(o=>o.status),['Created','Completed','Refunded','Refunded']);assert.deepEqual(votes,[true,false,true,false]);
assert.deepEqual(balances,{buyer:'39.0',seller:'61.0',escrow:'0.0'});assert.equal(allowance,'0');assert.equal(count,'4');
assert(BigInt(blocks.timeoutRefund.timestamp)>BigInt(orders[3].deadline));assert(BigInt(blocks.shippedConfirm.timestamp)>BigInt(orders[1].deadline));
const report={classification:'REAL_POTOS_BONUS_FINAL_SETTLEMENT_INDEPENDENTLY_VERIFIED',verifiedAt:new Date().toISOString(),chainId:60600,rpc:deploy.rpc,contractAddress:escrow,transactions:summaries,
  current:{startBlock:Number(BigInt(start[1])),finishBlock:Number(BigInt(finish.number)),chainTimeBeijing:beijing(BigInt(finish.timestamp)),orders,votes2:votes,orderCount:count,balances,allowance,readSemantics:'Separate latest calls within the stated block range; not an atomic historical snapshot'},
  evidenceLimits:['The conflicting-vote receipt independently contains only ArbitrationVoted and no token transfer or settlement; intermediate live balances and order status are separately preserved browser observations.',
    'The post-deadline refund(1) rejection was a simulation, not a mined failed transaction; its prior Shipped-state observation cannot be recreated using this RPC historical blockTag.',
    'Order 2 entered dispute before shipping; no shipped-then-disputed success is claimed.'],
  browserEvidence:{threadId:'6aef511b-a004-83e8-b504-940168a71c43',archive:'route_a_acceptance_checkpoint.zip',reportedOriginalImages:21,localOriginalArchiveReceived:false}};
for(const [name,value] of Object.entries({'final-settlement-verified-receipts':receipts,'final-settlement-verified-transactions':transactions,'final-settlement-verified-blocks':blocks,'final-settlement-verification':report}))fs.writeFileSync(path.join(dir,name+'.json'),JSON.stringify(value,null,2)+'\n');
console.log(JSON.stringify(report,null,2));
