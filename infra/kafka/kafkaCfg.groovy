setting {
    ka.hosts=["xly04","xly05"] as List
    zk.hosts=["xly01","xly02","xly03"] as List
    zk.client.port=12181
    ka.client.port = "9092"
    server.listeners="PLAINTEXT://:${ka.client.port}"
    server.log.dirs="/data0/kafka/data"
    server.num.partitions=2
    server.broker.id=ka.hosts.indexOf(currentHost)+1
    server.zookeeper.connect=zk.hosts.collect{"${it}:${zk.client.port}"}.join(",")+"/kafka"
    producer.bootstrap.servers=ka.hosts.collect{"${it}:${ka.client.port}"}.join(",")
    // v < 1.0
    //produce.metadata.broker.list=nodes.collect{"${it}:${server.port}"}.join(",")
    consumer.bootstrap.servers=ka.hosts.collect{"${it}:${ka.client.port}"}.join(",")
    //log4j.kafka.logs.dir="/data0/kafka/log"
    log4j.log4j.appender.kafkaAppender.MaxBackupIndex=3
    log4j.log4j.appender.kafkaAppender.MaxFileSize=10MB
    log4j.log4j.appender.stateChangeAppender.MaxBackupIndex=3
    log4j.log4j.appender.stateChangeAppender.MaxFileSize=10MB
    log4j.log4j.appender.requestAppender.MaxBackupIndex=3
    log4j.log4j.appender.requestAppender.MaxFileSize=10MB
    log4j.log4j.appender.cleanerAppender.MaxBackupIndex=3
    log4j.log4j.appender.cleanerAppender.MaxFileSize=10MB
    log4j.log4j.appender.controllerAppender.MaxBackupIndex=3
    log4j.log4j.appender.controllerAppender.MaxFileSize=10MB
    log4j.log4j.appender.authorizerAppender.MaxBackupIndex=3
    log4j.log4j.appender.authorizerAppender.MaxFileSize=10MB
}
