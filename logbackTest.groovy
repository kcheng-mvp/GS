#! /usr/bin/env groovy

import groovy.time.TimeCategory

import java.text.SimpleDateFormat


def currentPath = new File(getClass().protectionDomain.codeSource.location.path).parent
GroovyShell groovyShell = new GroovyShell(binding)
def logback = groovyShell.parse(new File(currentPath, "core/Logback.groovy"))


//def log = logback.getLogger("logbackTest", "/Users/kcheng")

//1509436457
def sdf = new SimpleDateFormat("yyyy-MM-dd-HH");
def dateString = "2017-11-01-00"

println sdf.parse(dateString).getTime()