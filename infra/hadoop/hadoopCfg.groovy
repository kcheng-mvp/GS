settings {
    hosts = ["xly01", "xly02", "xly03"] as List
    dataDirs = ["/data0/hadoop1", "/data0/hadoop2"] as List
}

conf {
    // secondary name node
    masters = ["xly02"]
    // dataNode and jobNode
    slaves = ["xly01", "xly02", "xly03"] as List

    'core-site.xml' {
        fs {
            'default' {
                name = "hdfs://${settings.hosts[0]}:9000"
            }
        }
        hadoop {
            tmp {
                dir = "${settings.dataDirs[0]}/tmp"
            }
        }
    }

    'hdfs-site.xml' {
        dfs {
            name {
                dir = "${settings.dataDirs[0]}/dfs/name"
            }
        }
        //list, separated by comma
        dfs {
            data {
                dir = settings.dataDirs.collect { "${it}/dfs/data" }.join(",")
            }
        }
        fs {
            checkpoint {
                dir = "${settings.dataDirs[0]}/dfs/namesecondary"
            }
        }
        dfs {
            secondary {
                http {
                    address = "${masters[0]}:50090"
                }
            }
        }
    }
    'mapred-site.xml' {
        mapred {
            job {
                tracker = "${settings.hosts[0]}:9001"
            }
        }
        //list, separated by comma
        mapred {
            local {
                dir = settings.dataDirs.collect { "${it}/mapred/local" }.join(",")
            }
        }
        mapred {
            system {
                dir = "${settings.dataDirs[0]}/mapred/system"
            }
        }
        mapreduce {
            jobtracker {
                staging {
                    root {
                        dir = "${settings.dataDirs[0]}/mapred/staging"
                    }
                }
            }
        }
    }

    'hadoop-env.sh' {
        HADOOP_PID_DIR = "${settings.dataDirs[0]}/logs"
        HADOOP_LOG_DIR = "${settings.dataDirs[0]}/pids"
        JAVA_HOME = "/usr/local/jdk"
    }
}

