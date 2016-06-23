#!/usr/bin/env groovy


def home= new File(System.getProperty("user.home"));
def url = "http://www.webhostingreviewjam.com/mirror/apache/zookeeper/";

def curl="/usr/bin/curl ${url}";
println curl;


def processBuilder = new ProcessBuilder(curl);
def process = processBuilder.redirectErrorStream(true).start();
process.inputStream.eachLine {println it}
processBuilder.redirectErrorStream(false);
process.waitFor();
if (process.exitValue()){
        println "Create domain successfully ......";
}

