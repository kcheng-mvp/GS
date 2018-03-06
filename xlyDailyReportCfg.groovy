settings {
    hdfsPath = "hdfs://hbhadoop101:9000/advdata/crmr"
    hdfsPathRegiser = "hdfs://hbhadoop101:9000/advdata/userregister"
    localPath = "/tmp"
    logPath = "/tmp"
    cron = "15 09 * * *"
     games= ["2016": "测试1",
     "201709": "测试2"
    ] as Map

    appids = ["2016052401435705": "2017090108502293", "2015122301031978":"2017090508566356", "2016052301432568": "2017090508566176"] as Map
}

mail {
    host = "smtp.exmail.qq.com"
    smtp.port = "465"
    smtp.auth = "true"
    transport.protocol = "smtp"
    smtp.ssl.enable = "true"
    smtp.user = "it.admin@163.com"
    smtp.password = "dddd"
    receiptor = "it.admin1@163.com"
}


