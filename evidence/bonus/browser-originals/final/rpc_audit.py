import json, urllib.request, datetime, pathlib, sys
RPC_URL="https://rpc-testnet.potos.hk"
ROOT=pathlib.Path("/workspace/scratch/4123b81611ec/route_a_acceptance")
ESCROW="0x917Dc833eadBa6544F1fd80498715da39Ac739f4"
TOKEN="0xB7dA837F93A5448ca0762B80862d053daf9715B3"
BUYER="0x5a435c43Ca006bE1688d0D7B95194327528EAB26"
SELLER="0xE0F0970f35FFE50dB6a6AD25ba775674D24682D2"
def rpc(method,params):
    req=urllib.request.Request(RPC_URL,data=json.dumps({"jsonrpc":"2.0","id":1,"method":method,"params":params}).encode(),headers={"Content-Type":"application/json"})
    with urllib.request.urlopen(req,timeout=25) as res: value=json.load(res)
    if "error" in value: raise RuntimeError(value["error"])
    return value["result"]
def batch(calls):
    result=[]
    for offset in range(0,len(calls),8):
        group=calls[offset:offset+8]
        qs=[{"jsonrpc":"2.0","id":i,"method":m,"params":p} for i,(m,p) in enumerate(group)]
        req=urllib.request.Request(RPC_URL,data=json.dumps(qs).encode(),headers={"Content-Type":"application/json"})
        with urllib.request.urlopen(req,timeout=25) as res: values=json.load(res)
        if not isinstance(values,list): raise RuntimeError(values)
        byid={r["id"]:r for r in values}
        for i in range(len(group)):
            value=byid[i]
            if "error" in value: raise RuntimeError(value["error"])
            result.append(value["result"])
    return result
def word(x):return hex(x)[2:].zfill(64)
def addr(x):return x[2:].lower().zfill(64)
def decode_order(raw):
    b=bytes.fromhex(raw[2:])
    num=lambda o:int.from_bytes(b[o:o+32],"big")
    base=num(0)
    values=[b[base+i*32:base+(i+1)*32] for i in range(7)]
    strings=base+int.from_bytes(values[6],"big"); length=num(strings)
    return {"buyer":"0x"+values[0][-20:].hex(),"seller":"0x"+values[1][-20:].hex(),"token":"0x"+values[2][-20:].hex(),"amount":str(int.from_bytes(values[3],"big")),"deadline":int.from_bytes(values[4],"big"),"status":int.from_bytes(values[5],"big"),"description":b[strings+32:strings+32+length].decode()}
def snapshot(prefix):
    before=rpc("eth_getBlockByNumber",["latest",False])
    calls=[("eth_chainId",[])]
    calls += [("eth_call",[{"to":ESCROW,"data":"0xd09ef241"+word(i)},"latest"]) for i in range(4)]
    calls += [("eth_call",[{"to":ESCROW,"data":"0xff981099"+word(2)},"latest"])]
    calls += [("eth_call",[{"to":TOKEN,"data":"0x70a08231"+addr(a)},"latest"]) for a in [BUYER,SELLER,ESCROW]]
    calls += [("eth_call",[{"to":TOKEN,"data":"0xdd62ed3e"+addr(BUYER)+addr(ESCROW)},"latest"]),("eth_getTransactionCount",[BUYER,"latest"]),("eth_getTransactionCount",[BUYER,"pending"])]
    values=batch(calls);after=rpc("eth_getBlockByNumber",["latest",False])
    data={"source":RPC_URL,"queryMode":"latest live reads; block timestamps bound observation window; historical block-state behavior not assumed","blockBefore":{k:before[k] for k in ["number","hash","timestamp"]},"blockAfter":{k:after[k] for k in ["number","hash","timestamp"]},"chainId":int(values[0],16),"orders":[dict(id=i,**decode_order(values[i+1])) for i in range(4)],"votes2":[bool(int(values[5][2+i*64:2+(i+1)*64],16)) for i in range(4)],"balances":{k:str(int(values[j],16)) for k,j in [("buyer",6),("seller",7),("escrow",8)]},"allowance":str(int(values[9],16)),"nonceLatest":int(values[10],16),"noncePending":int(values[11],16),"rawQueries":[{"method":m,"params":p} for m,p in calls],"rawResults":values}
    (ROOT/(prefix+"_state.json")).write_text(json.dumps(data,ensure_ascii=False,indent=2))
    return data
def find_nonce(nonce,minimum,prefix):
    head=int(rpc("eth_blockNumber",[]),16)
    for top in range(head,minimum-1,-8):
        nums=list(range(top,max(top-8,minimum-1),-1))
        blocks=batch([("eth_getBlockByNumber",[hex(n),True]) for n in nums])
        for b in blocks:
            for t in (b or {}).get("transactions",[]):
                if isinstance(t,dict) and t.get("from","").lower()==BUYER.lower() and int(t.get("nonce","0x0"),16)==nonce:
                    rc=rpc("eth_getTransactionReceipt",[t["hash"]])
                    data={"source":RPC_URL,"transaction":t,"receipt":rc,"block":{k:b[k] for k in ["number","hash","timestamp"]}}
                    (ROOT/(prefix+"_transaction_raw.json")).write_text(json.dumps(data,indent=2))
                    return data
        print("scanned",nums[0],nums[-1],flush=True)
    return None
if __name__=="__main__":
    prefix=sys.argv[1]
    if len(sys.argv)>2:
        tx=find_nonce(int(sys.argv[2]),int(sys.argv[3]),prefix)
        print("TX",json.dumps(tx),flush=True)
    state=snapshot(prefix)
    print("STATE",json.dumps({k:v for k,v in state.items() if k not in ["rawQueries","rawResults"]}),flush=True)

