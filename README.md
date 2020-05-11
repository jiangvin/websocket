## 基于springboot + websocket + html5 canvas打造网络版坦克大战

### 前言 ###
**该项目为本人业余时间原创，禁止任何一切商业行为，转载须经过本人同意，本人微信号: Jiang_Vin**

之前工作重心一直偏向后台微服务集群研究，业务项目页主要是单工通信为主，最近一直想扩展自己的技能，想用websocket技术做点东西。

1.websocket运用的极致就是即时战略游戏，因为我只有1个人，之前也没做过游戏，思来想去决定做 **网络版坦克大战**（即时性强，游戏逻辑简单，后续可以基于它继续入坑AI学习）。


2.既然要做就想要做好，之前因为工作关系前端也写得少，所以这次想利用html5 canvas写个 **电脑-手机-平板 的全平台支援游戏**，电脑用键盘控制，手机平板用触控控制。

3.既然是网络游戏，效率考验也是重要环节，所以这里想设计成**多游戏房间**模式，服务器可以创建不限数量的游戏房间，每个游戏房间能容纳不限数量的玩家进行游戏，来看看最终的运算效率和服务器承受计限到底如何。

4.关于游戏性。因为这是大学毕后做的第一个网络类型游戏，初期的想法很简单：游戏房间分为PVP PVE EVE三种模式，其中E为电脑AI（EVE模式也是为之后AI训练架设温床），玩家可以在**任何时间**加入或者离开房间，如果某个房间的玩家全部离开，则房间自动关闭，及时释放服务器资源。

坑挖得有点大，看之后能不能一步步填满，之前也租了服务器，之后定期会把master的代码部署到服务器上供测试：

**项目部署地址：** http://116.63.170.134/

### 账号系统 ###
既然是网络游戏，肯定需要账号系统，我这里设计得比较简单，所有用户都只需要一个**不重复**的用户名则可以登录，用户名可以支援各种语言。

**用户加入：**
一旦用户输用户名则websocket会通过url的形式带入后端，后端通过DefaultHandshakeHandler截获名字，通过HttpSessionHandshakeInterceptor进行websocket握手前检测名字是否重复，若不重复则通过ChannelInterceptor通知用户服务中心。

**用户离开：**
一样通过ChannelInterceptor通知用户服务中心。

**逻辑相关：**
一开始再想如何得知用户已经订阅完所有path并且确定加入成功？后面决定当前端完成所有加入的前置操作时会给后端发一个READY消息，当后端接收到了READY后则可以通知所有人: XXX加入了游戏并且给他初始化坦克。

**逻辑流程图：**

1.建立连接<br>
2.订阅地址<br>
3.暂停并发送CLIENT_READY<br>
4.接收游戏数据<br>
5.接收SERVER_READY并解除暂停<br>

**关于超时重连问题：**
在测试中发现有一定记录连接服务器会超时，这里前端加了一个行为判断，当连接超过5秒的时候结束锁定，并返回一个超时信息。因为后续的连接可能涉及到切换场景，所以这里的超时比较麻烦，先考虑设定成先暂停，再切换场景，最后再切换回来的操作，代码如下：

  Room.getOrCreateRoom(); //创建场景<br>
  Common.runNextStage();  //切换场景<br>
  Status.setStatus(Status.getStatusPause(), "加入房间中...");<br>
  Common.sendStompMessage({<br>
      "roomId": roomId,<br>
      "joinTeamType": selectGroup<br>
  }, "JOIN_ROOM");<br>
  Common.addConnectTimeoutEvent(function () {<br>
      Common.runLastStage(); //超时后又切换回来<br>
  });<br>
