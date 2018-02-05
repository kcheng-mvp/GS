setting {
    tickTime = 2000
    initLimit = 10
    syncLimit = 5
    clientPort = 12181
    serverPort = 12888
    leaderPort = 13888
    data.dir = "/data0/zookeeper/data"
    zkenv.ZOO_LOG_DIR="/data0/zookeeper/log"
    zkenv.ZOO_LOG4J_PROP="INFO,ROLLINGFILE"
    log4j.log4j.appender.ROLLINGFILE.MaxBackupIndex=20
    hosts=["xly01","xly02","xly03"] as List
}


