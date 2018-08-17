
/**
 * https://hadoop.apache.org/docs/r2.8.4/hadoop-project-dist/hadoop-hdfs/hdfs-default.xml
 */
/****
 *    Hadoop V2
 */
settings {
    hosts = ["xly01", "xly02", "xly03"] as List
    dataDirs = ["/data0/hadoop1", "/data0/hadoop2"] as List
    zkAddress = ["zk1:2181","zk2:2181","zk3:2181"] as List
    rmIds = ["rm1":"xly01","rm2":"xly02"] as Map
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
            namenode{
                name {
                    dir = "${settings.dataDirs[0]}/dfs/name"
                }
            }
            checkpoint {
                dir = "${settings.dataDirs[0]}/dfs/namesecondary"
            }
        }
        /**
         * Determines where on the **LOCAL FILESYSTEM** an DFS data node should store its blocks.
         * If this is a comma-delimited list of directories, then data will be stored in all named directories,
         * typically on different devices. Directories that do not exist are ignored.
         */
        dfs {
            datanode {
                data {
                    dir = settings.dataDirs.collect { "${it}/dfs/data" }.join(",")
                }
            }
        }

    }
    'mapred-site.xml' {
        /**
         * The **LOCAL DIRECTORY** where MapReduce stores intermediate data files.
         * May be a comma-separated list of directories on different devices in order to spread disk i/o.
         * Directories that do not exist are ignored.
         */
        mapreduce {
            framework {
                name = "yarn"
            }
            cluster {
                local {
                    dir = settings.dataDirs.collect { "${it}/mapred/local" }.join(",")
                }
            }

            jobtracker{
                /**
                 * The directory where MapReduce stores control files. Created by hadoop itself in hdfs
                 */
                system {
                    dir = "hadoop/mapred/system"
                }

                /**
                * The root of the staging area for users' job files In practice,
                * this should be the directory where users' home directories are located (usually /user)
                * Created by hadoop itself in hdfs.
                */

                staging {
                    root {
                        dir = "hadoop/mapred/staging"
                    }
                }
            }
        }
    }
    'yarn-site.xml' {
        yarn{
            resourcemanager{
                ha{
                    enabled=true
                    this."rm-ids"= "${settings.rmIds.keySet().collect { it }.join(",")}"
                }
                hostname {
                    settings.rmIds.each {k, v ->
                        this."${k}" = "${v}"
                    }
                }
                'cluster-id' {
                    "cluster1"
                }
                'zk-address' {
                    return settings.zkAddress.collect { it }.join(",")
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

