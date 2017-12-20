nodes {
    master = "xly01"
    second = "xly02"
    data = ["xly03"] as List
}


hadoopenv {
    log {
        dir = "/data0/hadoop1/logs"
    }
    pid {
        dir = "/data0/hadoop1/pids"
    }
}

coreSite {
    fs {
        'default' {
            name = "hdfs://${nodes.master}:9000"
        }
    }
    hadoop {
        tmp {
            dir = "/data0/hadoop1/tmp"
        }
    }
}
hdfsSite {
    dfs {
        name {
            dir = "/data0/hadoop1/dfs/name"
        }
    }
    //list, separated by comma
    dfs {
        data {
            dir = "/data0/hadoop1/dfs/data"
        }
    }
    fs {
        checkpoint {
            dir = "/data0/hadoop1/dfs/namesecondary"
        }
    }
    dfs {
        secondary {
            http {
                address = "${nodes.second}:50090"
            }
        }
    }
}
mapredSite {
    mapred {
        job {
            tracker = "${nodes.master}:9001"
        }
    }
    //list, separated by comma
    mapred {
        local {
            dir = "/data0/hadoop1/mapred"
        }
    }
    mapred {
        system {
            dir = "/data0/hadoop1/mapred/system"
        }
    }
    mapreduce {
        jobtracker {
            staging {
                root {
                    dir = "/data0/hadoop1/mapred/staging"
                }
            }
        }
    }
}
