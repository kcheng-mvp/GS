#! /usr/bin/env groovy



def currentPath = new File(getClass().protectionDomain.codeSource.location.path).parent
GroovyShell groovyShell = new GroovyShell(binding)
def logback = groovyShell.parse(new File(currentPath, "core/Logback.groovy"))


//def log = logback.getLogger("logbackTest", "/Users/kcheng")

def ms = 1509613382000
Calendar calendar = Calendar.getInstance();
calendar.setTimeInMillis(ms);

println calendar.getTime().toString()
//log.info("Hello")
//log.error("its bad")
