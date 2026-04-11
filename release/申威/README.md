### 目录说明
jdk cloudwave_server和aisearch用的linux版本jdk
cloudwave_server	数据库服务
aisearch			web服务

### 部署说明
1. 安装包放在英文路径下

### aisearch/conf/application.properties配置项说明：

1. your_model、your_key、your_url配置示例：
```
query.modelName=qwen3-32b
query.apiKey=sk-9cc9dbdd16b3488b9ad7ea695a
query.url=https://dashscope.aliyuncs.com/compatible-mode/v1
```
备注：your_model、your_key、your_url各要配置3个地方

2. your_input_dir配置示例：
kg.inputDir=/home/ubuntu/workspace_aisearch/input

备注：kg.inputDir是aisearch服务端存储上传的文档的路径，写上aisearch/input的绝对路径就行。

### cloudwave_server启停

1. 进入cloudwave_server/bin目录，执行 ./start.sh 启动，执行 ./stop.sh 关闭，nohup.out文件为日志文件。

### aisearch启停

1. 进入aisearch目录，执行 ./start.sh 启动，执行 ./stop.sh 关闭，nohup.out文件为日志文件。

2. 启动完成之后，服务端口为8082。