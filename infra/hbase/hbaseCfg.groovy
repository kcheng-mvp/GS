hbaseEnv{
    JAVA_HOME="/usr/local/jdk"
    //Add a pointer to your HADOOP_CONF_DIR to the HBASE_CLASSPATH environment variable in hbase-env.sh
    HBASE_CLASSPATH="/usr/local/hbase/conf"
    // default to /var/hbase/pid
    //HBASE_PID_DIR="/data0/hbase/pid"
}
//https://github.com/apache/hbase/blob/master/hbase-common/src/main/resources/hbase-default.xml
hbaseSite{
    hbase{
        rootdir="hdfs://xly01:9000/hbase"
        cluster{
            distributed=true
        }
        zookeeper{
            quorum = "xly01,xly02,xly03"
            property{
                dataDir = "/data0/hbase/zookeeper"
            }
        }
        regionserver{
            handler{
                count = 200
            }
        }
    }
}
regionservers = ["xly01","xly02","xly03","xly04"]
