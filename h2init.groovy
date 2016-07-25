#! /usr/bin/env groovy


import groovy.sql.Sql;


@Grab(group = 'com.h2database', module = 'h2', version = '1.3.175')
@GrabConfig(systemClassLoader = true)



def home = new File(System.getProperty("user.home"));
def dbHome = new File(home, ".h2");
def driver = "org.h2.Driver";
def url = "jdbc:h2:~/.h2/junk";


if (args.size() != 2) {
    println "eg : h2init.groovy 1.txt abc"
    return -1;
}

def file = new File(args[0]);
if (file.exists()) {
    def sqlCon = Sql.newInstance(url, "sa", "", driver);

    sqlCon.execute('''
	create table if not exists APKS (
	ID INTEGER IDENTITY,
	OWNER varchar(10),
	APP_NAME varchar(50),
	PKG_NAME varchar(50),
	INSTALLATION_TYPE INT
);
''');

    sqlCon.execute('''
	create table if not exists JUNK_DETAIL (
	ID INTEGER IDENTITY,
	OWNER varchar(10),
	PATH varchar(300),
	ROOT_PATH varchar(50)
);
''');

    def rootFolder = null;
    sqlCon.withTransaction {
        sqlCon.withBatch(100, 'INSERT INTO JUNK_DETAIL (OWNER, PATH, ROOT_PATH) values(?,?, ?) ') { stmt ->
            file.eachLine("UTF-8", { line ->
                if (line.indexOf("root:") > -1) {
                    rootFolder = ((line.split("root:"))[1]).trim();
                } else if (rootFolder && line.indexOf(rootFolder) > -1) {

                    String path = line.substring(line.indexOf(rootFolder) + rootFolder.length());
                    String[] items = path.split("/");
                    String rootPath = items[1];
//                    println("path -> ${path}, rootPath -> ${rootPath}");
                    stmt.addBatch(args[1], path, rootPath);
                }
            })
        }
    }

    def pkg= new File(args[0]);
    def type = -1;
    sqlCon.withTransaction {
        sqlCon.withBatch(20, 'INSERT INTO APKS (OWNER, APP_NAME, PKG_NAME,INSTALLATION_TYPE) values(?,?,?,?) ') { stmt ->
            pkg.eachLine("UTF-8", { line ->
                if (line.indexOf("Installed application") > -1) {
                    type = 1;
                } else if (line.indexOf("Pre-installed application") > -1) {
                    type = 0;
                } else if (type >= 0) {
                    String temp = (line.split("App name:"))[1];
                    String[] items = temp.split("-> Package name:");
                    stmt.addBatch(args[1], items[0].trim(), items[1].trim(),type);
                }
            })
        }
    }

    sqlCon.close();
}


