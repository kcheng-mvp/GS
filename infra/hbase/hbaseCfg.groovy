hbaseEnv{
    JAVA_HOME="/usr/jkd"
    //Add a pointer to your HADOOP_CONF_DIR to the HBASE_CLASSPATH environment variable in hbase-env.sh
    HBASE_CLASSPATH="hadoop_home/conf"
    HBASE_PID_DIR="hadoop_home/conf"
}
//https://github.com/apache/hbase/blob/master/hbase-common/src/main/resources/hbase-default.xml
hbaseSite{
    hbase{
        rootdir="hdfs://hbase001:9000/hbase"
        cluster{
            distributed=true
        }
        zookeeper{
            quorum = "hbase001,hbase002,hbase003"
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
regionservers = ""
