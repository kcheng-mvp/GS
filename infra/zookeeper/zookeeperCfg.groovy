settings {
    //@todo
    hosts = ["xly01", "xly02", "xly03"] as List
}

conf {
    'log4j.properties' {
        log4j.rootLogger="INFO, ROLLINGFILE"
        log4j.appender.ROLLINGFILE.MaxFileSize="10MB"
        log4j.appender.ROLLINGFILE.MaxBackupIndex = 20
    }
    'zoo.cfg' {
        tickTime = 2000
        initLimit = 10
        syncLimit = 5
        clientPort = 12181
        //@todo
        dataDir = "/data0/zookeeper/data"
        settings.hosts.eachWithIndex { s, idx ->
            setProperty("server.${idx + 1}", "${s}:12888:13888")
        }
    }
}
bin {
    'zkEnv.sh'{
        // zookeeper log4j folder(There is a minimal changes since v3.3, but you can always set the folder
        ZOO_LOG_DIR='"/data0/zookeeper/log"'
    }
}
scripts {
    settings.hosts.eachWithIndex { h, idx ->
        setProperty("zkCli-(${h}).sh", ["../bin/zkCli.sh -server ${settings.hosts[(idx + 1) % settings.hosts.size()]}:${conf.'zoo.cfg'.clientPort}"])
        setProperty("stat-(${h}).sh",["echo stat | nc ${settings.hosts[(idx + 1) % settings.hosts.size()]} ${conf.'zoo.cfg'.clientPort}"])
    }
}
