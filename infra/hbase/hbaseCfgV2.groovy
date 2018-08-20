conf {
    'hbase-env.sh' {
        setProperty("export JAVA_HOME","/usr/local/jdk")
        //Add a pointer to your HADOOP_CONF_DIR to the HBASE_CLASSPATH environment variable in hbase-env.sh
        setProperty("export HBASE_CLASSPATH","/usr/local/hadoop/conf")
        setProperty("export HBASE_PID_DIR","/var/hbase/pid")
        setProperty("export HBASE_LOG_DIR","/var/hbase/log")
    }
    //https://github.com/apache/hbase/blob/master/hbase-common/src/main/resources/hbase-default.xml
    'hbase-site.xml' {
        hbase {
            this."tmp.dir" = "/data0/hbase-local"
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
            }
            regionserver {
                handler {
                    count = 200
                }
            }
        }
    }
    //@todo
    regionservers=["xly01", "xly02", "xly03", "xly04"]

}
