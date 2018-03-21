mail {
    host = "smtp.exmail.qq.com"
    smtp.port = "465"
    smtp.auth = "true"
    transport.protocol = "smtp"
    smtp.ssl.enable = "true"
    smtp.user = "it.admin@163.com"
    smtp.password = "1234"
    receiptor = "it.admin@163.com, it1.admin@163.com"
}

services {

    netre {
        check = "pgrep netre"
        restart = "sudo systemctl restart netre"
    }

    tomcat {
        check = "ps -ef | grep netre | grep java"
        restart = "sudo systemctl restart netre"
    }

}
