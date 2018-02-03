settings {
    tickTime = 2000
    initLimit = 10
    syncLimit = 5
    clientPort = 12181
    serverPort = 12888
    leaderPort = 13888
    dataDir = "/data0/zookeeper/data"
    log4j = "/data0/zookeeper/log"
    traceLog= "/data0/zookeeper/trace"
    hosts=["xly01","xly02","xly03"] as List
}


