nodes {
    master = "xly01"
    second = "xly02"
    data = ["xly03"] as List
}


hadoopenv {
    paths = ["/data0/hadoop1", "/data1/hadoop1","/data2/hadoop1"] as List
    log {
        dir = "${paths[0]}/logs"
    }
    pid {
        dir = "${paths[0]}/pids"
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
            dir = "${hadoopenv.paths[0]}/tmp"
        }
    }
}

hdfsSite {
    dfs {
        name {
            dir = "${hadoopenv.paths[0]}/dfs/name"
        }
    }
    //list, separated by comma
    dfs {
        data {
            dir = hadoopenv.paths.collect{ "${it}/dfs/data"}.join(",")
        }
    }
    fs {
        checkpoint {
            dir = "${hadoopenv.paths[0]}/dfs/namesecondary"
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
            dir = hadoopenv.paths.collect{ "${it}/mapred/local"}.join(",")
        }
    }
    mapred {
        system {
            dir = "${hadoopenv.paths[0]}/mapred/system"
        }
    }
    mapreduce {
        jobtracker {
            staging {
                root {
                    dir = "${hadoopenv.paths[0]}/mapred/staging"
                }
            }
        }
    }
}
