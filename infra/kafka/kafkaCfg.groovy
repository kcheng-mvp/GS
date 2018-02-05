setting {
    ka.hosts=["xly04","xly05"] as List
    zk.hosts=["xly01","xly02","xly03"] as List
    zk.client.port=12181
    ka.client.port = "9092"
    server.listeners="PLAINTEXT://:${ka.client.port}"
    server.log.dirs="/data0/kafka/data"
    server.num.partitions=2
    server.broker.id=ka.hosts.indexOf(currentHost)+1
    server.zookeeper.connect=zk.hosts.collect{"${it}:${ka.client.port}"}.join(",")
    producer.bootstrap.servers=ka.hosts.collect{"${it}:${ka.client.port}"}.join(",")
    // v < 1.0
    //produce.metadata.broker.list=nodes.collect{"${it}:${server.port}"}.join(",")
    consumer.bootstrap.servers=ka.hosts.collect{"${it}:${ka.client.port}"}.join(",")
    log4j.kafka.logs.dir="/data0/kafka/log"
}
