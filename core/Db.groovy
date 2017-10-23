#! /usr/bin/env groovy


import groovy.sql.Sql;


@Grab(group = 'com.h2database', module = 'h2', version = '1.3.175')
@GrabConfig(systemClassLoader = true)


//
//def home = new File(System.getProperty("user.home"));
//def dbHome = new File(home, ".h2");
//def driver = "org.h2.Driver";



def h2Con(String dbName) {
    def driver = "org.h2.Driver";
    Sql.newInstance("jdbc:h2:~/.h2/${dbName}", "sa", "", driver);
}

