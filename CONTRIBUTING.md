## 1. 如何下载项目源代码?
    git clone https://gitlab.com/sdnlab/jaguar.git
    或者
    git clone git@gitlab.com:sdnlab/jaguar.git

    也可以直接在jaguar项目页面直接点击仓库地址后的下载按钮,下载.zip或.gz格式的源码压缩包

## 2. 如何编译源代码?
    在项目的根目录下,执行mvn clean install
    如果由于单元测试问题导致编译不通过,可以执行 mvn clean install -DskipTests

## 3. 该项目如何启动?需要哪些基本配置?
    编译成功后,进入到目录 karaf/target/assembly/bin/ 执行./karaf启动版本
    版本启动后,通过浏览器登陆web界面,通过restconf修改k8s apiserver的IP地址和端口号
    YANG模块是k8s-apiserver-config


    已经验证可连接K8S 1.6版本,计划在10月份发布版本里支持K8S 1.10版本

## 4. 如何提交故障和需求?
    登陆https://gitlab.com 
    打开https://gitlab.com/sdnlab/jaguar
    左侧Issues, 编辑提交你的问题
    
## 5. 如何参与功能特性的设计和开发？
    登陆https://gitlab.com 
    打开https://gitlab.com/sdnlab/jaguar
    左侧Issues列表或看板里，选择感兴趣的功能，在comments里填写对功能的理解及大概实现思路
    开会讨论确定实现方案，并确定主要参与者
    本地创建分支开发代码，代码git push origin yourbranch 提交,提交完成后，创建Merge_Request，待Code Review通过后，即完成功能开发
    
## 6. 本地修改代码后,如何验证?
    当前需要在本地搭建一套K8S,才能进行验证.
    SDNLAB已提供一套三节点的云服务器集群,这套环境里部署K8S,大家提交代码后会触发自动测试验证

## 7. 如何提交代码?
    项目的Developer clone项目代码后,请本地创建分支(git branch yourbranch),在该分支上完成修改并push到gitlab (git push origin yourbranch)
    然后登陆gitlab,创建Merge_Request,代码审核通过后,变更会被合并到matser分支
    
具体操作可参考下[gitlab help](https://gitlab.com/help)
或者[使用gitlab做Code Review](http://www.360doc.com/content/16/0920/17/1073512_592302821.shtml)