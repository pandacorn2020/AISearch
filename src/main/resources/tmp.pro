spring.datasource.url=jdbc:wisdomdata:@127.0.0.1:1978
#spring.datasource.url=jdbc:wisdomdata:@aimedicine.cloudwave.cn:1978
spring.datasource.username=system
spring.datasource.password=CHANGEME
spring.datasource.driver-class-name=com.wisdomdata.jdbc.CloudDriver
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect


spring.datasource.type=org.springframework.jdbc.datasource.SimpleDriverDataSource

spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=50MB

# llm Configuration
# Qwen qwen2.5-72b-instruct  qwen2.5-32b-instruct  qwen2.5-14b-instruct qwen-plus qwen-turbo
kg.modelName=Qwen3-32B-local
kg.apiKey=sk-9cc9
kg.url=https://model-inference-8ef84e18.a.user.hmlt.ltaidc.com:8444/v1
#kg.inputDir=I:\\aisearch\\release_0623002\\release_0623002\\knowledge\\test_speed
kg.inputDir=/opt/workspace_aisearch/release_aiserach_1113/input

# Qwen qwen2.5-72b-instruct  qwen2.5-32b-instruct  qwen2.5-14b-instruct qwen-plus qwen-turbo qwen3-32b
query.modelName=Qwen3-32B-local
query.apiKey=sk-9
query.url=https://model-inference-8ef84e18.a.user.hmlt.ltaidc.com:8444/v1

# Qwen qwen2.5-72b-instruct  qwen2.5-32b-instruct  qwen2.5-14b-instruct qwen-plus qwen-turbo
report.modelName=Qwen3-32B-local
report.apiKey=sk-9cc9
report.url=https://model-inference-8ef84e18.a.user.hmlt.ltaidc.com:8444/v1

# deepseek deepseek-chat
#llm.modelName=deepseek-chat
#llm.apiKey=sk-bd1d69fb8c5f48369d46a44755227e77
#llm.url=https://api.deepseek.com/v1

# ??????
#webserver.imageUrlPrefix=http://aimedicine.cloudwave.cn/aisearch/image/
webserver.imageUrlPrefix=http://app.bitleap.cn:8085/aisearch/image/


cloudwave.server=127.0.0.1
cloudwave.port=1978
cloudwave.user=system
cloudwave.password=CHANGEME

# ?? Hibernate ??? SQL
spring.jpa.show-sql=true

# ??? SQL ??
spring.jpa.properties.hibernate.format_sql=true

# ??????
logging.level.org.hibernate.type.descriptor.sql=TRACE