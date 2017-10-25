#!/usr/bin/env groovy

//ssh dev1 'grep `hostname` /etc/hosts'


def exec(shell, String... host) {

    def commands = shell.split("\\t+") as List;
    if (host) {
        commands.add(0, host[0])
        commands.add(0, "ssh")
    } else {
        commands.add(shell)
    }

    def processBuilder = new ProcessBuilder(commands);
    def process = processBuilder.redirectErrorStream(true).start();
    def rt = [];
    process.inputStream.eachLine {
        rt << it
//        println it
    }
    process.waitFor();
    if (process.exitValue()) {
        println "successfully ......";
    }
    rt

}

