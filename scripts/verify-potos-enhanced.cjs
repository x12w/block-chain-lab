#!/usr/bin/env node
// Read-only verification of the single enhanced-contract deployment.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const {execFileSync} = require('node:child_process');
const {Interface, AbiCoder, formatUnits, getCreateAddress} = require(process.env.ETHERS_MODULE || 'ethers');
const root = path.resolve(__dirname, '..');
const dir = path.join(root, 'evidence/bonus/potos');
const deployment = JSON.parse(fs.readFileSync(path.join(dir, 'deployment-http.json')));
const result = deployment.body;
assert.equal(deployment.httpStatus, 200);
const abi = new Interface(JSON.parse(fs.readFileSync(path.join(root, 'build/resources/main/solidity/EscrowEnhanced.abi'))));
const token = new Interface(['function balanceOf(address) view returns (uint256)',
  'function allowance(address,address) view returns (uint256)']);
const tokenAddress = '0xB7dA837F93A5448ca0762B80862d053daf9715B3';
const seller = '0xE0F0970f35FFE50dB6a6AD25ba775674D24682D2';
const rpcUrl = 'https://rpc-testnet.potos.hk';
const same = (a,b) => assert.equal(a.toLowerCase(), b.toLowerCase());
function rpc(method, params) {
  const response = JSON.parse(execFileSync('curl', ['--fail-with-body','--silent','--show-error',
    '--max-time','30','--tlsv1.2','--tls-max','1.2','--retry','2','--retry-all-errors',
    rpcUrl,'-H','Content-Type: application/json','--data-binary','@-'],
    {input:JSON.stringify({jsonrpc:'2.0',id:1,method,params}),encoding:'utf8'}));
  if (response.error) throw Error(JSON.stringify(response.error));
  return response.result;
}
function call(address, iface, method, args=[]) {
  return iface.decodeFunctionResult(method, rpc('eth_call', [{to:address,data:iface.encodeFunctionData(method,args)},'latest']));
}
assert.equal(BigInt(rpc('eth_chainId', [])), 60600n);
const receipt = rpc('eth_getTransactionReceipt', [result.transactionHash]);
const tx = rpc('eth_getTransactionByHash', [result.transactionHash]);
assert(receipt && tx);
assert.equal(receipt.status,'0x1');
assert.equal(tx.to,null);
same(tx.from,deployment.firstArbiter);
same(receipt.from,deployment.firstArbiter);
same(receipt.transactionHash,result.transactionHash);
same(receipt.contractAddress,result.contractAddress);
same(getCreateAddress({from:tx.from,nonce:BigInt(tx.nonce)}),result.contractAddress);
assert.equal(BigInt(tx.value),0n);
assert.equal(BigInt(tx.chainId),60600n);
assert.equal(BigInt(receipt.blockNumber).toString(),result.deployBlock);
const binary = fs.readFileSync(path.join(root,'build/resources/main/solidity/EscrowEnhanced.bin'),'utf8').trim().replace(/^0x/,'');
const args = AbiCoder.defaultAbiCoder().encode(['address','address'],[deployment.firstArbiter,deployment.secondArbiter]);
assert.equal(tx.input.toLowerCase(), ('0x'+binary+args.slice(2)).toLowerCase());
const code = rpc('eth_getCode',[result.contractAddress,'latest']);
assert(code && code !== '0x');
const block = rpc('eth_getBlockByNumber',[receipt.blockNumber,false]);
const startBlock = Number(BigInt(rpc('eth_blockNumber',[])));
assert.equal(call(result.contractAddress,abi,'version')[0],2n);
same(call(result.contractAddress,abi,'arbiter')[0],deployment.firstArbiter);
same(call(result.contractAddress,abi,'secondArbiter')[0],deployment.secondArbiter);
const orderCount = call(result.contractAddress,abi,'orderCount')[0].toString();
const balances = Object.fromEntries(Object.entries({buyer:deployment.secondArbiter,seller,escrow:result.contractAddress})
  .map(([role,address])=>[role,formatUnits(call(tokenAddress,token,'balanceOf',[address])[0],18)]));
const allowance = call(tokenAddress,token,'allowance',[deployment.secondArbiter,result.contractAddress])[0].toString();
const finishBlock = Number(BigInt(rpc('eth_blockNumber',[])));
const report = {classification:'REAL_POTOS_ENHANCED_DEPLOYMENT_INDEPENDENTLY_VERIFIED',
  verifiedAt:new Date().toISOString(),chainId:60600,rpc:rpcUrl,contractAddress:result.contractAddress,
  transactionHash:result.transactionHash,deployBlock:Number(result.deployBlock),
  deployedAt:new Date(Number(BigInt(block.timestamp))*1000).toISOString(),
  firstArbiter:deployment.firstArbiter,secondArbiter:deployment.secondArbiter,
  firstOperator:deployment.firstOperator,secondOperator:deployment.secondOperator,
  status:receipt.status,gasUsed:BigInt(receipt.gasUsed).toString(),
  effectiveGasPrice:BigInt(receipt.effectiveGasPrice).toString(),
  executionFeePOT:formatUnits(BigInt(receipt.gasUsed)*BigInt(receipt.effectiveGasPrice),18),
  constructorAndCompiledCreationBytecodeMatch:true,version:'2',deployedCodeBytes:(code.length-2)/2,
  current:{startBlock,finishBlock,orderCount,token:tokenAddress,balances,buyerAllowance:allowance,
    readSemantics:'Sequential latest reads, not an atomic historical snapshot'},
  remaining:'Buyer orders/deposits, seller shipping/dispute, refund boundary and both arbitrator votes still require actual testnet execution'};
fs.writeFileSync(path.join(dir,'deployment-verified-receipts.json'),JSON.stringify({deployment:receipt},null,2)+'\n');
fs.writeFileSync(path.join(dir,'deployment-verified-transaction.json'),JSON.stringify(Object.fromEntries(
  ['hash','blockHash','blockNumber','from','to','input','value','gas','gasPrice','nonce','chainId'].filter(k=>k in tx).map(k=>[k,tx[k]])),null,2)+'\n');
fs.writeFileSync(path.join(dir,'deployment-verification.json'),JSON.stringify(report,null,2)+'\n');
console.log(JSON.stringify(report,null,2));
