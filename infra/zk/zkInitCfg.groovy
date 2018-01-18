settings {
    tickTime = 2000
    initLimit = 10
    syncLimit = 5
    clientPort = 12181
    serverPort = 12888
    leaderPort = 13888
    dataDir = "/data0/zookeeper/data"
    server=["xly01","xly02","xly03"] as List
}
/*
server.1=zoo1:2888:3888
 */

