conf {
    'hbase-env.sh' {
        setProperty("export JAVA_HOME","/usr/local/jdk")
        //Add a pointer to your HADOOP_CONF_DIR to the HBASE_CLASSPATH environment variable in hbase-env.sh
        setProperty("export HBASE_CLASSPATH","/usr/local/hadoop/conf")
        setProperty("export HBASE_PID_DIR","/data0/hbase/pids")
        setProperty("export HBASE_LOG_DIR","/data0/hbase/logs")
        /**
         * Use the HBASE_MANAGES_ZK variable in conf/hbase-env.sh. This variable, which defaults to true,
         * tells HBase whether to start/stop the ZooKeeper ensemble servers as part of HBase start/stop
         */
        //@todo
        setProperty("export HBASE_MANAGES_ZK",false)
    }

    /**
     * hbase master high available settings. this should keep all the nodes which want to start master
     * To configure backup Masters, create a new file in the conf/ directory which will be distributed across your cluster,
     * called backup-masters. For each backup Master you wish to start,
     * add a new line with the hostname where the Master should be started.
     * Each host that will run a Master needs to have all of the configuration files available.
     * In general, it is a good practice to distribute the entire conf/ directory across all cluster nodes.
     */
    this."backup-masters" = ["xly01","xly02"] as List

    //https://github.com/apache/hbase/blob/master/hbase-common/src/main/resources/hbase-default.xml
    'hbase-site.xml' {
        hbase {
            this."tmp.dir" = "/data0/hbase-tmp"
            rootdir = "hdfs://hdcluster:8020/hbase"
            cluster {
                distributed = true
            }
            zookeeper {
                //@todo
                quorum = "xly01,xly02,xly03"
                //@todo keep this just for embedded zookeeper server
                this."property.dataDir" = "/data0/hbase/zookeeper"
                //hbase.zookeeper.property.clientPort
                //@todo
                this.property.clientPort = 12181
            }
            regionserver {
                /**
                 * Count of RPC Listener instances spun up on RegionServers. Same property is used by the Master for count of master handlers.
                 * Too many handlers can be counter-productive. Make it a multiple of CPU count.
                 * If mostly read-only, handlers count close to cpu count does well. Start with ****twice**** the CPU count and tune from there.
                 */
                handler.count = 32
            }

        }
    }
    //@todo
    regionservers=["xly01", "xly02", "xly03", "xly04"]

}
