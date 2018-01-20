#!/usr/bin/env groovy

//ssh dev1 'grep `hostname` /etc/hosts'

def exec(shell, String... host) {

    def commands = shell.split() as List;

    //for remote shell
    if (host) {
        commands.add(0, host[0])
        commands.add(0, "ssh")
    }

//    println "[Command ] >> : ${commands} "

    def processBuilder = new ProcessBuilder(commands);
    def process = processBuilder.redirectErrorStream(true).start();
    def rt = [] as List;
    process.inputStream.eachLine {
//        println it
        rt << it
    }
    process.waitFor();

    ["code": process.exitValue(), "msg":rt]
}


def sshug(String host){
    def rt = exec("id -u -n", host)
    def user = rt.msg[0]
    rt = exec("id -g -n", host)
    def group = rt.msg[0]
    ["u": user, "g":group]
}
