#! /usr/bin/env groovy

import static groovyx.net.http.ContentType.JSON
import groovy.time.TimeCategory
import groovyx.net.http.HTTPBuilder

@Grab(group = 'org.codehaus.groovy.modules.http-builder', module = 'http-builder', version = '0.5.0')

def currentPath = new File(getClass().protectionDomain.codeSource.location.path).parent
GroovyShell groovyShell = new GroovyShell()
def cron4j = groovyShell.parse(new File(currentPath, "core/Cron4J.groovy"))
def digestUtils = groovyShell.parse(new File(currentPath, "core/DigestUtils.groovy"))

def properties = new Properties()
properties.load(getClass().getResourceAsStream('restGet.properties'));


def url = properties.get("url")
def appid = properties.get("key1")

def key = properties.get("key2")

def hourlyRegister = {
    def sign = digestUtils.md5("appid=${appid}&key=${key}")
    println sign

    def start = Calendar.getInstance().getTime();
    use(TimeCategory) {
        start = start - 1.hours
        start.set(minute: 0, second: 0, millisecond: 0)
    }


    start = start.getTime() ;


    def end = start + 60 * 60 * 1000
    def http = new HTTPBuilder(url)

    http.get( path : '/api/xlygetpuid',
            contentType : JSON,
            query : [start_time:(start/1000) as Long, end_time:(end/1000) as Long,sign:sign] ) { resp, reader ->

        println "response status: ${resp.statusLine}"
        println 'Headers: -----------'
        resp.headers.each { h ->
            println " ${h.name} : ${h.value}"
        }
        println 'Response data: -----'
        System.out << reader
        println '\n--------------------'
    }

} as Runnable

def cron = "05 * * * *"
cron4j.start(cron, hourlyRegister)

//hourlyRegister()

