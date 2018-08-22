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
    //https://github.com/apache/hbase/blob/master/hbase-common/src/main/resources/hbase-default.xml
    'hbase-site.xml' {
        hbase {
            this."tmp.dir" = "/data0/hbase-tmp"
            rootdir = "hdfs://hdcluster/hbase"
            cluster {
                distributed = true
            }
            zookeeper {
                //@todo
                quorum = "xly01,xly02,xly03"
                property {
                    dataDir = "/data0/hbase/zookeeper"
                }
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
