#! /usr/bin/env groovy
import java.text.SimpleDateFormat

def scriptDir = new File(getClass().protectionDomain.codeSource.location.path).parent
GroovyShell groovyShell = new GroovyShell()
def mailMan = groovyShell.parse(new File(scriptDir, "../core/Mailman.groovy"))
def shell = groovyShell.parse(new File(scriptDir, "../core/Shell.groovy"))
def config = new File(scriptDir, 'config.groovy')


class GroovyTimerTask extends TimerTask {
    Closure closure

    void run() {
        closure()
    }
}

class TimerMethods {
    static TimerTask runEvery(Timer timer, long delay, long period, Closure codeToRun) {
        TimerTask task = new GroovyTimerTask(closure: codeToRun)
        timer.schedule task, delay, period
        task
    }
}

def serviceConfig = new ConfigSlurper().parse(config.text).get("services").flatten();
def services = serviceConfig.keySet().collect { return it.split("\\.")[0] } as Set

def sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")

def monitor = {
    for (String service : services) {
        def command = serviceConfig["${service}.check"]
        if(!shell.exec(command)['code']){
            command = serviceConfig["${service}.restart"]
            def rt = shell.exec(command)
            mailMan.sendMail("Restart ${services} at ${sdf.format(Calendar.getInstance().getTime())}",rt['msg'] , config)
        }
    }
}


use (TimerMethods) {
    def timer = new Timer()
    def task = timer.runEvery(1000, 2000) {
        monitor()
    }
}



