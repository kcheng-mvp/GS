
/**
 * https://hadoop.apache.org/docs/r1.0.4/hdfs-default.html
 * https://hadoop.apache.org/docs/r1.0.4/core-default.html
 * https://hadoop.apache.org/docs/r1.0.4/mapred-default.html
 */

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
        /**
         * Determines where on the **LOCAL FILESYSTEM** the DFS name node should store the name table(fsimage).
         * If this is a comma-delimited list of directories then the name table is replicated in all of the directories,
         * for redundancy
         * */
        dfs {
            name {

                dir = "${settings.dataDirs[0]}/dfs/name"
            }
        }
        /**
         * Determines where on the **LOCAL FILESYSTEM** an DFS data node should store its blocks.
         * If this is a comma-delimited list of directories, then data will be stored in all named directories,
         * typically on different devices. Directories that do not exist are ignored.
         */
        dfs {
            data {
                dir = settings.dataDirs.collect { "${it}/dfs/data" }.join(",")
            }
        }
        /**
         * Determines where on the **LOCAL FILESYSTEM** the DFS secondary name node should store the temporary images to merge.
         * If this is a comma-delimited list of directories then the image is replicated in all of the directories for redundancy.
         */
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
        /**
         * The **LOCAL DIRECTORY** where MapReduce stores intermediate data files.
         * May be a comma-separated list of directories on different devices in order to spread disk i/o.
         * Directories that do not exist are ignored.
         */
        mapred {
            local {
                dir = settings.dataDirs.collect { "${it}/mapred/local" }.join(",")
            }
        }
        /**
         * The directory where MapReduce stores control files. Created by hadoop itself in hdfs
         */
        mapred {
            system {
                dir = "/hadoop/mapred/system"
            }
        }
        /**
         * The root of the staging area for users' job files In practice,
         * this should be the directory where users' home directories are located (usually /user)
         * Created by hadoop itself in hdfs.
         */
        mapreduce {
            jobtracker {
                staging {
                    root {
                        dir = "/hadoop/mapred/staging"
                    }
                }
            }
        }
    }

    'hadoop-env.sh' {
        setProperty("export HADOOP_PID_DIR","${settings.dataDirs[0]}/logs")
        setProperty("export HADOOP_LOG_DIR","${settings.dataDirs[0]}/pids")
        setProperty("export JAVA_HOME","/usr/local/jdk")
    }
}

