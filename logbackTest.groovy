#! /usr/bin/env groovy

import groovy.time.TimeCategory

import java.text.SimpleDateFormat


def currentPath = new File(getClass().protectionDomain.codeSource.location.path).parent
GroovyShell groovyShell = new GroovyShell(binding)
def logback = groovyShell.parse(new File(currentPath, "core/Logback.groovy"))


//def log = logback.getLogger("logbackTest", "/Users/kcheng")

//1509436457
def sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
def ms =1509436457000
//        1509613382000
def calendar = Calendar.getInstance();

calendar.setTimeInMillis(ms);
//
println calendar.getTime().toString()

println sdf.format(calendar.getTime())

/*

SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
Date date  = sdf.parse("2017/11/09");
Calendar cal = Calendar.getInstance();
cal.setTimeInMillis(date.getTime());
cal.set(Calendar.HOUR_OF_DAY, 0); //set hours to zero
cal.set(Calendar.MINUTE, 0); // set minutes to zero
cal.set(Calendar.SECOND, 0); //set seconds to zero
Long start = cal.getTimeInMillis()/1000;
Long end = start + 24*60*60;

println start
println end

println ((1510243200 - 1510217199)/3600)
/*
1510217199

1510156800
1510243200
*/





