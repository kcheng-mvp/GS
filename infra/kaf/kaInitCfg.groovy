settings {
    ka.server=["xly04","xly05"] as List
    zk.server=["xly01","xly02","xly03"] as List
    server.port = "9092"
    server.listeners="PLAINTEXT://:${server.port}"
    server.log.dirs="/data0/kafka/data"
    server.num.partitions=2
    server.zookeeper.connect=zk.server.collect{"${it}:12181"}.join(",")
    log4j.kafka.logs.dir="/data0/kafka/log"
    producer.bootstrap.servers=ka.server.collect{"${it}:${server.port}"}.join(",")
    //produce.metadata.broker.list=nodes.collect{"${it}:${server.port}"}.join(",")
    consumer.bootstrap.servers=ka.server.collect{"${it}:${server.port}"}.join(",")
}
