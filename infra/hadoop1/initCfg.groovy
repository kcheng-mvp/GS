nodes {
    master = "xly01"
    second = "xly02"
    data = ["xly03"] as List
}


hadoopenv {
    root = "/data0/hadoop1"
    log {
        dir = "${root}/logs"
    }
    pid {
        dir = "${root}/pids"
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
            dir = "${hadoopenv.root}/tmp"
        }
    }
}
hdfsSite {
    dfs {
        name {
            dir = "${hadoopenv.root}/dfs/name"
        }
    }
    //list, separated by comma
    dfs {
        data {
            dir = "${hadoopenv.root}/dfs/data"
        }
    }
    fs {
        checkpoint {
            dir = "${hadoopenv.root}/dfs/namesecondary"
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
            dir = "${hadoopenv.root}/mapred/local"
        }
    }
    mapred {
        system {
            dir = "${hadoopenv.root}/mapred/system"
        }
    }
    mapreduce {
        jobtracker {
            staging {
                root {
                    dir = "${hadoopenv.root}/mapred/staging"
                }
            }
        }
    }
}
