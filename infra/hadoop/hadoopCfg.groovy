setting {
    hosts = ["xly01","xly02","xly03"] as List
    dataVols = ["/data0/hadoop1","/data0/hadoop2"] as List
}

coreSite {
    fs {
        'default' {
            name = "hdfs://${setting.hosts[0]}:9000"
        }
    }
    hadoop {
        tmp {
            dir = "${setting.dataVols[0]}/tmp"
        }
    }
}

hdfsSite {
    dfs {
        name {
            dir = "${setting.dataVols[0]}/dfs/name"
        }
    }
    //list, separated by comma
    dfs {
        data {
            dir = setting.dataVols.collect{ "${it}/dfs/data"}.join(",")
        }
    }
    fs {
        checkpoint {
            dir = "${setting.dataVols[0]}/dfs/namesecondary"
        }
    }
    dfs {
        secondary {
            http {
                address = "${setting.hosts[1]}:50090"
            }
        }
    }
}
mapredSite {
    mapred {
        job {
            tracker = "${setting.hosts[0]}:9001"
        }
    }
    //list, separated by comma
    mapred {
        local {
            dir = setting.dataVols.collect{ "${it}/mapred/local"}.join(",")
        }
    }
    mapred {
        system {
            dir = "${setting.dataVols[0]}/mapred/system"
        }
    }
    mapreduce {
        jobtracker {
            staging {
                root {
                    dir = "${setting.dataVols[0]}/mapred/staging"
                }
            }
        }
    }
}
hadoopEnv {
    HADOOP_PID_DIR="${setting.dataVols[0]}/logs"
    HADOOP_LOG_DIR="${setting.dataVols[0]}/pids"
    JAVA_HOME="/usr/lib/j2sdk1.5-sun"
}
