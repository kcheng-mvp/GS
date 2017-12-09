settings {
    hdfsPath = "hdfs://hbhadoop101:9000/advdata/crmr"
    localPath = "/tmp"
    logPath = "/tmp"
    cron = "15 09 * * *"
     games= ["2016": "测试1",
     "201709": "测试2"
    ] as Map
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


