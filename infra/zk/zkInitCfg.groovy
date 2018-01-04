setting {
    tickTime = 2000
    initLimit = 10
    syncLimit = 5
    clientPort = 12181
    dataDir = "/data0/zookeeper/data"
    server=["xly01","xly02","xly03"] as List
}
server.1 = hbweb101: 12888 : 13888
server.2 = hbweb102: 12888 : 13888
server.3 = hbweb103: 12888 : 13888

