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

        //todo -XX:ParallelGCThreads=8+(logical processors-8)(5/8)
        /**
         *  Refer to https://www.evernote.com/l/Abna63xMNrNAH6zzaMiSr0zpWXf_3bJNDOE
         * 16 cpu then 8 + (16-8) * 5/8 = 8 + 5 = 13
         *
         *  32GB heap, -XX:G1NewSizePercent=3;
         *  64GB heap, -XX:G1NewSizePercent=2;
         *  100GB and above heap, -XX:G1NewSizePercent=1;
         *  as we are using 32g heap, so it should be '-XX:G1NewSizePercent=3'
         */
        setProperty("export HBASE_REGIONSERVER_OPTS",'"-XX:+UseG1GC -Xms32g -Xmx32g -XX:NewSize=3g -XX:MaxNewSize=3g  -XX:MaxGCPauseMillis=150 -XX:+ParallelRefProcEnabled -XX:-ResizePLAB -XX:ParallelGCThreads=13"')
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


    'log4j.properties' {
        log4j.rootLogger="INFO, DFRA"
    }

}
