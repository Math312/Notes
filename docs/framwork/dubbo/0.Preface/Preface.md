# Preface

Dubbo作为一个RPC框架，现已被国内广泛应用，在2020年，常见的RPC框架主要有如下几种，简单列举一下各个框架的对比：

||开发语言|服务治理|多种序列化|多种注册中心|管理中心|跨语言通讯|整体性能|
|---|---|---|---|---|---|---|---|
|Dubbo|Java|✔|✔|✔|✔|✘|3|
|Maton|Java|✔|✔|✔|✔|✘|4|
|Thrift|跨语言|✘|只支持thrift|✘|✘|✔|5|
|Grpc|跨语言|✘|只支持protobuf|✘|✘|✔|3|

本系列文章主要介绍Dubbo。

在介绍Dubbo前，笔者首先对论文[Implementing Remote Procedure Calls](http://web.eecs.umich.edu/~mosharaf/Readings/RPC.pdf)进行简单的介绍，论文的原文翻译在[如下链接](../../../paper/rpc/Implementing%20Remote%20Procedure%20Calls.md)。