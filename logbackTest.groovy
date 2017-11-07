#! /usr/bin/env groovy

import groovy.time.TimeCategory


def currentPath = new File(getClass().protectionDomain.codeSource.location.path).parent
GroovyShell groovyShell = new GroovyShell(binding)
def logback = groovyShell.parse(new File(currentPath, "core/Logback.groovy"))


//def log = logback.getLogger("logbackTest", "/Users/kcheng")

def ms = 1509613382000
def time = Calendar.getInstance().getTime();

use(TimeCategory){
    time = time - 3.days
}
//calendar.setTimeInMillis(ms);
//
//println calendar.getTime().toString()

time.toString()





