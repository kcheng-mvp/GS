// Please refer to https://www.evernote.com/shard/s441/nl/77222586/abdddd4f-1d72-4d42-83d4-03afdd5c4ecf/
settings {
    //@todo
    ka.hosts = ["xly04", "xly05"] as List
    //@todo
    zk.hosts = ["xly01", "xly02", "xly03"] as List
    zk.client.port = 12181
    ka.client.port = 9092
    zk.connect = settings.zk.hosts.collect { "${it}:${settings.zk.client.port}" }.join(",") + "/kafka"
    //@todo
    ka.dataDirs = ["/data0/kafka/data","/data1/kafka/data","/data2/kafka/data","/data3/kafka/data"] as List
}
// please refer to https://www.evernote.com/l/Abl60pq7lmJEzYG9GWNj5aytAUl9NMbFN-8

// XFS
config {
    settings.ka.hosts.eachWithIndex { host, idx ->
        "server-(${host}).properties" {
            listeners = "PLAINTEXT://:${settings.ka.client.port}"
            log.dirs = settings.ka.dataDirs.collect { it }.join(",")
            /**
             * The default number of log partitions per topic,
             * number of partition default as the number of the 2 * ka_hosts
             * Required partitions = Max (T/P, T/C)
             */
            //@todo default as : 2 * ka_hosts
            num.partitions = settings.ka.hosts.size() * 2
            broker.id = settings.ka.hosts.indexOf(host) + 1
            zookeeper.connect = settings.zk.connect

            //@todo for 3 node
            offsets.topic.replication.factor=3
            transaction.state.log.replication.factor=3
            transaction.state.log.min.isr=2

            log.retention.hours=24


        }

    }

    'producer.properties' {
        bootstrap.servers = settings.ka.hosts.collect { "${it}:${settings.ka.client.port}" }.join(",")
    }

    'consumer.properties' {
        bootstrap.servers = settings.ka.hosts.collect { "${it}:${settings.ka.client.port}" }.join(",")
    }

    'log4j.properties' {
        log4j.appender.kafkaAppender.MaxBackupIndex = 3
        log4j.appender.kafkaAppender.MaxFileSize = "10MB"
        log4j.appender.stateChangeAppender.MaxBackupIndex = 3
        log4j.appender.stateChangeAppender.MaxFileSize = "10MB"
        log4j.appender.requestAppender.MaxBackupIndex = 3
        log4j.appender.requestAppender.MaxFileSize = "10MB"
        log4j.appender.cleanerAppender.MaxBackupIndex = 3
        log4j.appender.cleanerAppender.MaxFileSize = "10MB"
        log4j.appender.controllerAppender.MaxBackupIndex = 3
        log4j.appender.controllerAppender.MaxFileSize = "10MB"
        log4j.appender.authorizerAppender.MaxBackupIndex = 3
        log4j.appender.authorizerAppender.MaxFileSize = "10MB"
    }

}

bin {
    'kafka-run-class.sh' {
        // set kafka log4j folder
        LOG_DIR = '"/data0/kafka/log"'
        // 64 ram/ 32 cpu
        KAFKA_HEAP_OPTS='"-Xms6g -Xmx6g"'
        // KAFKA_JVM_PERFORMANCE_OPTS='"-server -XX:MetaspaceSize=96m XX:+UseG1GC -XX:MaxGCPauseMillis=20 -XX:InitiatingHeapOccupancyPercent=35 ' +
        //        '-XX:G1HeapRegionSize=16M -XX:MinMetaspaceFreeRatio=50 -XX:MaxMetaspaceFreeRatio=80"'
    }

}

scripts {
    setProperty("kafka-topics.sh", ["../bin/kafka-topics.sh  --zookeeper ${settings.zk.connect}"])
    setProperty("kafka-console-producer.sh", ["../bin/kafka-console-producer.sh --broker-list ${config.'producer.properties'.bootstrap.servers}"])
    setProperty("kafka-console-consumer.sh", ["../bin/kafka-console-consumer.sh --bootstrap-server ${config.'producer.properties'.bootstrap.servers}"])
    setProperty("kafka-consumer-groups.sh", ["../bin/kafka-consumer-groups.sh --zookeeper ${settings.zk.connect}"])
}

