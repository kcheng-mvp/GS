settings {
    hosts = ["xly01", "xly02", "xly03"] as List
}

conf {
    'zookeeper-env.sh' {
        ZOO_LOG_DIR = "\$ZOOKEEPER_PREFIX/logs"
        ZOO_LOG4J_PROP = "INFO,ROLLINGFILE"
    }
    'log4j.properties' {
        log4j.appender.ROLLINGFILE.MaxBackupIndex = 20
    }
    'zoo.cfg' {
        tickTime = 2000
        initLimit = 10
        syncLimit = 5
        clientPort = 12181
        dataDir = "/data0/zookeeper/data"
        settings.hosts.eachWithIndex { s, idx ->
            setProperty("server.${idx+1}","${s}:12888:13888")
        }
    }

}
