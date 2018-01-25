settings {
    ka.port = 9092
    ka.log.dirs = "/data0/zookeeper/data"
    ka.server = ["xly01","xly02","xly03"] as List
    zk.server=["xly04","xly05"] as List
    zk.clientPort = 12181
}


