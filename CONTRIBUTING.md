## 1. 如何下载源代码?
git clone https://gitlab.com/sdnlab/jaguar.git/

## 2. 如何编译源代码?
在项目的根目录下,执行mvn clean install
如果由于单元测试问题导致编译不通过,可以先执行 mvn clean install -DskipTests

## 3. 该项目如何启动?需要哪些基本配置?
编译成功后,进入到目录 karaf/target/assembly/bin/ 执行./karaf启动版本
版本启动后,通过浏览器登陆web界面,通过restconf修改k8s apiserver的IP地址和端口号
YANG模块是k8s-apiserver-config


已经验证可连接K8S 1.6版本,计划在10月份发布版本里支持K8S 1.10版本

## 4. 如何提交故障和需求?
登陆https://gitlab.com,打开https://gitlab.com/sdnlab/jaguar
左侧Issues编辑提交你的问题

## 5. 本地修改代码后,如何验证?
当前需要在本地搭建一套K8S,才能进行验证.
后续SDNLAB计划提供一套三节点的云服务器集群,在这套环境里部署K8S供大家测试,验证使用

## 6. 如何提交代码?
具体流程还在梳理,8月1号前发布一份代码提交指导说明.