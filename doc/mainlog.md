## 主线日志分析

### 过滤日志
在callsdk 1.0.3以后的版本中，可通过关键字*Key_Procedure*自己过滤日志进行简单分析及问题定位


### 发起方日志

```C
1. ILiveSDK初始化日志
ILiveSDK: Key_Procedure|initSdk->init appid:1400013700, accountType:7285

2. CallSDK初始化日志
ILVB-CallMgr: Key_Procedure|init entered

3. 登录日志
ILVBLogin: Key_Procedure|ILVB-iLiveLogin strart |id:xiao
ILVBLogin: Key_Procedure|ILVB-iLiveLogin|login success

4. 发起呼叫日志
ILVB-CallMgr: Key_Procedure|VideoCall|makeCall to:green, type:2
ILVBRoom: Key_Procedure|ILVB-Room|start create room:1575029805 enter with im:true|video:true

5. 初始化渲染控件日志
ILVBRoom: Key_Procedure|ILVB-Room|init root view
AVVideoGroup: Key_Procedure|ILVB-AVVideoGroup|init sub views

6. 被叫接听日志
ILVB-CallMgr: Key_Procedure|CallMgr->Accept by green

7. 被叫挂断日志
ILVB-CallMgr: Key_Procedure|CallMgr->Hangup by green

8. 结束通话日志
ILVB-CallMgr: Key_Procedure|ILVB-Call|endCallEx enter:1575029805, 4, Remote cancel
ILVBRoom: Key_Procedure|ILVB-Room|exit avroom  Complete
```

### 接收方日志
```C
1. ILiveSDK初始化日志
ILiveSDK: Key_Procedure|initSdk->init appid:1400013700, accountType:7285

2. CallSDK初始化日志
ILVB-CallMgr: Key_Procedure|init entered

3. 登录日志
ILVBLogin: Key_Procedure|ILVB-iLiveLogin strart |id:xiao
ILVBLogin: Key_Procedure|ILVB-iLiveLogin|login success

4. 收到来电日志
ILVB-CallMgr: Key_Procedure|ILVB-Call|processIncoming->new call from:green|1575038522

5.接听来电日志
ILVB-CallMgr: Key_Procedure|VideoCall|acceptCall enter:1575038522
ILVBRoom: Key_Procedure|joinRoom->id: 1575038522 isIMsupport: true

6. 初始化渲染控件日志
ILVBRoom: Key_Procedure|ILVB-Room|init root view
AVVideoGroup: Key_Procedure|ILVB-AVVideoGroup|init sub views

7. 远程挂断日志
ILVB-CallMgr: Key_Procedure|CallMgr->Hangup by green

8. 结束通话日志
ILVB-CallMgr: Key_Procedure|ILVB-Call|endCallEx enter:1575038522, 4, Remote cancel
ILVBRoom: Key_Procedure|ILVB-Room|exit avroom  Complete
```
