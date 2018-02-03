setting {
    ka.hosts=["xly04","xly05"] as List
    zk.hosts=["xly01","xly02","xly03"] as List
    zk.client.port=12181
    server.port = "9092"
    server.listeners="PLAINTEXT://:${server.port}"
    server.log.dirs="/data0/kafka/data"
    server.num.partitions=2
    server.zookeeper.connect=zk.hosts.collect{"${it}:${zk.client.port}"}.join(",")
    log4j.kafka.logs.dir="/data0/kafka/log"
    producer.bootstrap.servers=ka.hosts.collect{"${it}:${server.port}"}.join(",")
    //produce.metadata.broker.list=nodes.collect{"${it}:${server.port}"}.join(",")
    consumer.bootstrap.servers=ka.hosts.collect{"${it}:${server.port}"}.join(",")
}
