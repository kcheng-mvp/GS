hadoopenv {
    masterNode = "xly01"
    secondNode = "xly02"
    dataNode = ["xly03"] as List
    dataVols = ["/data0/hadoop1"] as List
    log {
        dir = "${dataVols[0]}/logs"
    }
    pid {
        dir = "${dataVols[0]}/pids"
    }
}

coreSite {
    fs {
        'default' {
            name = "hdfs://${hadoopenv.masterNode}:9000"
        }
    }
    hadoop {
        tmp {
            dir = "${hadoopenv.dataVols[0]}/tmp"
        }
    }
}

hdfsSite {
    dfs {
        name {
            dir = "${hadoopenv.dataVols[0]}/dfs/name"
        }
    }
    //list, separated by comma
    dfs {
        data {
            dir = hadoopenv.dataVols.collect{ "${it}/dfs/data"}.join(",")
        }
    }
    fs {
        checkpoint {
            dir = "${hadoopenv.dataVols[0]}/dfs/namesecondary"
        }
    }
    dfs {
        secondary {
            http {
                address = "${hadoopenv.secondNode}:50090"
            }
        }
    }
}
mapredSite {
    mapred {
        job {
            tracker = "${hadoopenv.masterNode}:9001"
        }
    }
    //list, separated by comma
    mapred {
        local {
            dir = hadoopenv.dataVols.collect{ "${it}/mapred/local"}.join(",")
        }
    }
    mapred {
        system {
            dir = "${hadoopenv.dataVols[0]}/mapred/system"
        }
    }
    mapreduce {
        jobtracker {
            staging {
                root {
                    dir = "${hadoopenv.dataVols[0]}/mapred/staging"
                }
            }
        }
    }
}
