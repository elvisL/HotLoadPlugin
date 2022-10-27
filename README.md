# HotLoadPlugin
1. 二次开发需要按照自己公司的方式获取到对应的应用IP，现在是通过ZK的方式
2. ServiceNode是应用的数据模型，可以按照自己公司进行调整
3. 远程JVM需要打开端口,如Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8000,现在是写死的，可以按需要自己调整