hadoopenv {
    masterNode = "xly01"
    secondNode = "xly02"
    dataNode = ["xly03"] as List
    dataVols = ["/data0/hadoop1","/data0/hadoop2"] as List
}

core-site {
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

hdfs-site {
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
mapred-site {
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
hadoop-en {
    HADOOP_PID_DIR="${dataVols[0]}/logs"
    HADOOP_LOG_DIR="${dataVols[0]}/pids"
    JAVA_HOME="/usr/lib/j2sdk1.5-sun"
}
