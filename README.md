![](https://img1.sdnlab.com/wp-content/uploads/2018/07/21jaguar_01_668x400.jpg)

# Introduction
JAGUAR项目是一个基于开源的SDN控制器OpenDaylight的Kubernetes网络解决方案.

JAGUAR is a SDN solusion for Kubernetes's network based on OpenDaylight.

## Background
Kubernetes is a well-known container orchestration engine framework and is gaining more adoption as container deployments are in the rise. The aim of this project is to integrate Kubernetes and OpenDaylight in a seamless manner.
目前Kubernetes的网络实现都还谈不上是比较成熟的SDN，因此基于OpenDaylight为Kubernetes提供一个可用的SDN实现是非常有意义的.

## HOW TO BUILD
In project root path,execute command:
> mvn clean install
> or
> mvn clean install -DskipTests

## Configure and Run

编译成功后,进入到目录 karaf/target/assembly/bin/ 执行./karaf启动版本

版本启动后,通过浏览器登陆web界面,通过restconf修改k8s apiserver的IP地址和端口号

YANG模块是k8s-apiserver-config


已经验证可连接K8S 1.6版本,计划在10月份发布版本里支持K8S 1.10版本


## FAQ

### 1 为什么会成立这个开源项目？

JAGUAR项目缘起于OpenDaylight课程的教学尝试，希望通过从零到一搭建一个开源项目来让学员真正了解开源文化，也能够在这个过程中真正全面的丰富知识、锻炼技能。然而一个开源项目没有真实生产需求也就注定只能是玩具，随后我们对JAGUAR的愿景也就变得更加丰富起来，期望通过SDN技术更好的解决生产环境中的问题，同时也能寓教于项目。

### 2 项目的适用场景

我们将JAGUAR的第一个场景选择为容器集群网络，一是由于容器技术应用广泛，凭借其优秀的性能和高度的灵活性也获得的诸多开发者青睐，二是容器集群的网络方案一直不尽如人意，往往只能提供简单的连接功能，需要产生一些创新。考虑到Kubernetes的大范围使用，其网络实现还谈不上是比较成熟的SDN，我们最终将Solution确定为基于OpenDaylight提供Kubernetes的网络解决方案。下图是JAGUAR在kubernetes中的示例。

![](https://img1.sdnlab.com/wp-content/uploads/2018/07/21jaguar&kubernetes_02.jpg)

### 3 项目初期规划

初期的规划是能满足Kubernetes对网络的三条基本需求:
* i.每个POD拥有一个独立IP，所有POD都可以在不用NAT的方式下同别的POD通讯
* ii.所有Node都可以在不用NAT的方式下同所有POD通讯
* iii.POD的IP地址和别人看到的地址是同一个地址，可以通过这个IP地址对POD进行管理监控

### 4 第一个版本为什么选择基于ODL的Carbon版本开发？

我们开始考虑建立这样一个开源项目是去年底，当时ODL社区发布的最新版本是碳和氮，氮版本是karaf从3.0升级到4.0的第一个版本，经过简单验证，碳版本确实也相对更稳定一些，因此第一个版本就选择了碳版本。
待10月份发布第一个版本后，如果验证氧版本能更好的满足后续的规划需求，便规划ODL的版本升级到氧。

### 5 为什么选择ovs+vxlan构建K8S的网络？

虽然docker在主机内的容器网络默认是linux bridge，但ovs相较于linux bridge提供了更多的协议支持和管理方式，支持OpenFlow，考虑到后续方案演进的灵活性和可能性，ovs更适合采用SDN的实现方式。

vxlan是目前主流的Overlay网络实现技术，在灵活性，可扩展性，部署简单性，成熟度上相较于同类技术都有优势，基于上述因素，我们选择了vxlan方案。
下图是JAGUAR网络实现原理。

![](https://img1.sdnlab.com/wp-content/uploads/2018/07/21jaguar_network_03.jpg)

### 6 为什么不考虑优先在ODL社区立项？

目前希望了解到更多国内的场景和用户需求，所以单独建立并管理开源项目在规划和技术方向选择上有更大的自主性。

### 7 未来版本规划是怎样的？

计划在今年10月份发布第一个具备基本功能的版本，后续的规划希望大家一起参与讨论，共同建设社区。

### 8 这个开源项目采用的开源许可协议是什么？

项目是基于ODL开发的，而且有些地方还直接引用了ODL的代码，因此本项目沿用了ODL的EPL-1.0协议(Eclipse Public License 1.0)。

### 9 参与这个项目需要具备哪些背景知识和开发技能？

通过这个项目你会了解到kubernetes,docker,odl ,ovs,ovsdb, openflow等开源项目和相关协议技术，但不要求大家一定要具备这些项目背景才可以参与，只要你感兴趣，就欢迎参与。

### 10 如何参与这个项目？

如果你想参与这个项目，你唯一需要做的是在gitlab上注册一个账号，并填写表格申请成为开发者。

表格链接：http://ideapark.mikecrm.com/5MSrW8M 


