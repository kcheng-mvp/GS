#! /usr/bin/env groovy

@Grapes([
        @Grab(group = 'it.sauronsoftware.cron4j', module = 'cron4j', version = '2.2.5'),
        @Grab(group = 'commons-net', module = 'commons-net', version = '3.5'),
        @Grab(group = 'com.google.guava', module = 'guava', version = '21.0'),
        @GrabConfig(systemClassLoader = true),
        @Grab(group = 'com.h2database', module = 'h2', version = '1.3.175')
])


import com.google.common.hash.Hashing;
import com.google.common.io.Files
import it.sauronsoftware.cron4j.Scheduler
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTP


def home = new File(System.getProperty("user.home"))




