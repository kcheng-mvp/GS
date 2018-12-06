
/**
 * https://hadoop.apache.org/docs/r2.8.4/hadoop-project-dist/hadoop-hdfs/hdfs-default.xml
 * https://hadoop.apache.org/docs/r2.7.4/hadoop-mapreduce-client/hadoop-mapreduce-client-core/mapred-default.xml
 * https://hadoop.apache.org/docs/r2.7.5/hadoop-yarn/hadoop-yarn-common/yarn-default.xml
 * https://www.evernote.com/l/AbkS20fpTRtOE4F26R0GJvKljFsCl_SEIso
 */
/****
 *    Hadoop V2
 *
 */
settings {

    //@todo
    hosts = ["prd-hadoop01", "prd-hadoop02", "prd-hadoop03","prd-hadoop04","prd-hadoop05"] as List

    //@todo
    dataDirs = ["/data0/hadoop", "/data1/hadoop","/data2/hadoop", "/data3/hadoop",
    "/data4/hadoop", "/data5/hadoop","/data6/hadoop", "/data7/hadoop"] as List
    //@todo
    nameDirs = ["/data0/hadoop1", "/data0/hadoop2"] as List

    // Just a logic name, so you can set it any value
    nameserviceID="hdcluster"

    // name node id -> host map, key is just a logic name
    //@todo
    nameNodeIdHostMap = ["nn1":"xly01", "nn2":"xly02"] as Map
    nameNodeRPCPort = "8020"
    nameNodeHttpPort = "50070"

    // JournalNode,must be odd number
    //@todo
    journalNodes = ["xly01","xly02","xly03"] as List


    //@todo
    zkAddress = ["xly01:12181","xly02:12181","xly03:12181"] as List

    //@todo
    rmIds = ["rm1":"xly01","rm2":"xly02"] as Map
}

conf {

    // data node
    // @todo
    slaves = ["xly01", "xly02", "xly03"] as List

    'core-site.xml' {
        // fs.defaultFS
        fs {
            defaultFS = "hdfs://${settings.nameserviceID}"
        }

        // hadoop.tmp.dir
        hadoop {
            tmp {
                dir = "${settings.dataDirs[0]}/hadoop-tmpdir"
            }
        }

        // zk ha

        ha {
            zookeeper {
                quorum = settings.zkAddress.collect { it }.join(",")
            }
        }
    }


    'hdfs-site.xml' {

        dfs {
            //  the logical name for this new nameservice
            nameservices = "${settings.nameserviceID}"

            /**
             * dfs.replication
             * Default block replication. The actual number of replications can be specified when the file is created.
             * The default is used if replication is not specified in create time. Default is 3
             */
            replication = 2

            ha {
                //  dfs.ha.namenodes.[nameservice ID]
                namenodes {
                    this."${nameservices}" = settings.nameNodeIdHostMap.keySet().collect { it }.join(",")
                }
                this."automatic-failover" {
                    enabled = true
                }


                // dfs.ha.fencing.methods - a list of scripts or Java classes which will be used to fence the Active NameNode during a failover
                fencing {
                    methods = "sshfence"
                    this."ssh.private-key-files" = "/home/hadoop/.ssh/id_rsa"
                    this."ssh.connect-timeout" = 30000
                }

            }
            namenode {

                // the fully-qualified RPC address for each NameNode to listen on
                // dfs.namenode.rpc-address.mycluster
                "rpc-address" {
                    "${nameservices}" {
                        settings.nameNodeIdHostMap.each { k, v ->
                            this."${k}" = "${v}:${settings.nameNodeRPCPort}"
                        }
                    }
                }

                // the fully-qualified HTTP address for each NameNode to listen on
                // dfs.namenode.http-address.mycluster
                "http-address" {
                    "${nameservices}" {
                        settings.nameNodeIdHostMap.each { k, v ->
                            this."${k}" = "${v}:${settings.nameNodeHttpPort}"
                        }
                    }
                }

                //dfs.namenode.shared.edits.dir
                //the default port for the JournalNode is 8485
                shared {
                    edits {
                        dir = "qjournal://" + settings.journalNodes.collect {
                            "${it}:8485"
                        }.join(";") + "/${nameservices}"
                    }
                }

                // dfs.namenode.name.dir
                /**
                 * Determines where on the local filesystem the DFS name node should store the name table(fsimage).
                 * If this is a comma-delimited list of directories
                 * then the name table is replicated in all of the directories, for redundancy.
                 */
                this."name.dir" = settings.nameDirs.collect { "file://${it}/dfs/name" }.join(",")

                /**
                 * The number of Namenode RPC server threads that listen to requests from clients.
                 * If dfs.namenode.servicerpc-address is not configured then Namenode RPC server threads
                 * listen to requests from all nodes.
                 * Refer to https://www.evernote.com/l/AblzkvH9bRxMJ4JvFGcqfjgfK6iAPTx-EWo
                 */
                this."handler.count" = 20 * (Integer)(Math.log(settings.hosts.size()) / Math.log(2))

                /*
                // dfs.namenode.checkpoint.dir
                checkpoint {
                    dir = settings.dataDirs.collect { "file://${it}/dfs/namesecondary" }.join(",")
                }
                */


            }
            datanode {
                //@todo
                this."handler.count" = num_of_cpu_core
                // dfs.datanode.data.dir
                this."data.dir" = settings.dataDirs.collect { "file://${it}/dfs/data" }.join(",")
            }

            // dfs.client.failover.proxy.provider.[nameservice ID]
            this."client.failover.proxy.provider.${nameservices}" = "org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider"


            // dfs.journalnode.edits.dir - the path where the JournalNode daemon will store its local state
            // can not start with file:/
            this."journalnode.edits.dir" = "${conf.'core-site.xml'.hadoop.tmp.dir}/journal/local/data"

        }

    }

    /** Optional **/
    'mapred-site.xml' {
        /**
         * The **LOCAL DIRECTORY** where MapReduce stores intermediate data files.
         * May be a comma-separated list of directories on different devices in order to spread disk i/o.
         * Directories that do not exist are ignored.
         */
        mapreduce {
            framework {
                // mapreduce.framework.name
                name = "yarn"
            }
            cluster {
                local {
                    /**
                     * mapreduce.cluster.local.dir
                     * The local directory where MapReduce stores intermediate data files.
                     * May be a comma-separated list of directories on different devices in order to spread disk i/o.
                     * Directories that do not exist are ignored.
                     */
                    dir = settings.dataDirs.collect { "${it}/mapred/local" }.join(",")
                }
            }

            jobtracker{
                /**
                 * The directory where MapReduce stores control files. Created by hadoop itself in hdfs
                 */
                system {
                    // mapreduce.jobtracker.system.dir
                    dir = "${settings.dataDirs[0]}/mapred/system"
                }

                /**
                * The root of the staging area for users' job files In practice,
                * this should be the directory where users' home directories are located (usually /user)
                * Created by hadoop itself in hdfs.
                */

                staging {
                    root {
                        // mapreduce.jobtracker.staging.root.dir
                        dir = "${settings.dataDirs[0]}/mapred/staging"
                    }
                }
            }
        }
    }
    // https://hadoop.apache.org/docs/r2.7.5/hadoop-yarn/hadoop-yarn-common/yarn-default.xml
    // resourcemanager and  nodemanager should run with different user with hadoop
    'yarn-site.xml' {
        yarn{
            resourcemanager{
                ha{
                    //yarn.resourcemanager.ha.enabled
                    enabled=true
                    //yarn.resourcemanager.ha.rm-ids
                    this."rm-ids"= "${settings.rmIds.keySet().collect { it }.join(",")}"
                }
                //yarn.resourcemanager.hostname
                hostname {
                    settings.rmIds.each {k, v ->
                        this."${k}" = "${v}"
                    }
                }
                //yarn.resourcemanager.webapp.address
                this."webapp.address" {
                    settings.rmIds.each {k, v ->
                        this."${k}" = "${v}:8088"
                    }
                }
                //yarn.resourcemanager.cluster-id
                this."cluster-id" = "cluster1"

                //yarn.resourcemanager.zk-address
                this."zk-address" = settings.zkAddress.collect { it }.join(",")

                //To configure the ResourceManager to use the CapacityScheduler, set the following property in the conf/yarn-site.xml
                this.scheduler.class = "org.apache.hadoop.yarn.server.resourcemanager.scheduler.capacity.CapacityScheduler"

            }
            nodemanager{
                // mb
                /**
                 * Amount of physical memory, in MB, that can be allocated for containers.
                 * 8G
                 */
                //@todo
                this.'resource.memory-mb'=8192
                //@todo
                this.'resource.cpu-vcores'=8
                //@todo
                this.'resource.percentage-physical-cpu-limit'=40
            }
        }
    }
    /** Optional **/

    'hadoop-env.sh' {
        setProperty("export HADOOP_PID_DIR","${settings.dataDirs[0]}/pids")
        setProperty("export HADOOP_LOG_DIR","${settings.dataDirs[0]}/logs")
        setProperty("export JAVA_HOME","/usr/local/jdk")
        //https://www.evernote.com/l/Abl5eVXVYDRMgK07MThA2yh5s-JAanEpbmU
        //@todo
        setProperty("export HADOOP_NAMENODE_OPTS",'"-server -XX:ParallelGCThreads=8 -XX:+UseConcMarkSweepGC -XX:NewSize=800m -XX:MaxNewSize=800m -Xms6144m -Xmx6144m -XX:PermSize=128m -XX:MaxPermSize=256m"')
    }
}

