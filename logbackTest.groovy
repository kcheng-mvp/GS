#! /usr/bin/env groovy



def currentPath = new File(getClass().protectionDomain.codeSource.location.path).parent
GroovyShell groovyShell = new GroovyShell(binding)
def logback = groovyShell.parse(new File(currentPath, "core/Logback.groovy"))


def log = logback.getLogger("logbackTest", "/Users/kcheng")

log.info("Hello")
log.error("its bad")
