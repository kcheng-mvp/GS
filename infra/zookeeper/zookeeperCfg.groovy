setting {
    zooCfg.tickTime = 2000
    zooCfg.initLimit = 10
    zooCfg.syncLimit = 5
    zooCfg.clientPort = 12181
    zooCfg.dataDir = "/data0/zookeeper/data"
    serverPort = 12888
    leaderPort = 13888
    zkenv.ZOO_LOG_DIR="/data0/zookeeper/log"
    zkenv.ZOO_LOG4J_PROP="INFO,ROLLINGFILE"
    log4j.log4j.appender.ROLLINGFILE.MaxBackupIndex=20
    hosts=["xly01","xly02","xly03"] as List
}


