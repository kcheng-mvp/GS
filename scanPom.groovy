#!/usr/bin/env groovy




@Grab(group='com.h2database', module='h2', version='1.3.174')
@GrabConfig(systemClassLoader=true)
@Grab(group='jaxen', module='jaxen', version='1.1.4')
@GrabExclude('xml-apis:xml-apis')

import javax.xml.parsers.DocumentBuilderFactory;
import  groovy.sql.Sql;


if(args.size() < 1){
	println "Please input the folder"
	return -1;
}


def home= new File(System.getProperty("user.home"));
def dbHome = new File(home,".h2");
def driver ="org.h2.Driver";
def url = "jdbc:h2:~/.h2/pom";


def sqlCon = Sql.newInstance(url,"sa","",driver);

def init = {
	
	sqlCon.execute("drop table if exists pom;");
	def rs = sqlCon.execute('''
	create table pom (
	ID INTEGER IDENTITY,
	file varchar(150),
	groupId varchar(50),
	artifactId varchar(60),
	version varchar(20),
	scope varchar(20)
);
''');
	//sqlCon.close();
	
}


def result = new File(home,"pom.csv");
result = new FileOutputStream(result);
def bw = new BufferedWriter(new OutputStreamWriter(result,"UTF-8"));
bw.write("File|groupId|artifactId |version |scope|Status")
bw.newLine();


if(args && args[1] =="1"){
	init()
}

def folder = new File(args[0]);

folder.eachFileRecurse{ f ->
	if (!f.isDirectory() && f.name == "pom.xml"){
		def path= f.getPath();
		def pom = new XmlParser().parse(f)

		def map = new HashMap();
		//println path;
		if(pom.properties){
			pom.properties[0].each{
				//println it.name().getLocalPart();
				//println it.text();
				map.put("\${"+it.name().getLocalPart()+"}",it.text())
			}
		}
		//println map;
		
		if(pom.dependencies){
			pom.dependencies[0].dependency.each{ d ->

				def line =new StringBuffer(path);
				line.append("|");
				line.append(d.groupId.text());
				line.append("|")
				//println d.groupId.text()

				line.append(d.artifactId.text());
				line.append("|")

				//println d.artifactId.text();
				def v = d.version.text();
				if(map.get(v)) v = map.get(v);
				line.append(v);
				

				//println d.version.text();
				if(d.scope.text()){
					line.append("|")
					line.append(d.scope.text());
				}
				//println d.scope.text();
				//bw.write(line.toString());
				//bw.newLine();
				sqlCon.execute("insert into pom(file,groupId, artifactId,version,scope) values(?,?,?,?,?)",[path,d.groupId.text(),d.artifactId.text(),v,d.scope.text()]);
			}
		}
	}

}

def report = {

	def set = new HashSet();

	def query ='''
	select count(distinct version), artifactid
	from pom
	group by artifactid
	having count(distinct version) > 1
	'''

	sqlCon.eachRow(query){row ->
		set.add(row["artifactid"]);
	}
	query ='''
		select artifactid, count (distinct groupid)
		from pom
		group by artifactid 
		having count (distinct groupid) > 1
	'''

	sqlCon.eachRow(query){row ->
		set.add(row["artifactid"]);
	}

	query ="SELECT file,groupid,ARTIFACTID,vERSION,scope FROM POM order by ARTIFACTID,GROUPID  ,vERSION";
	sqlCon.eachRow(query){row ->
		def line = new StringBuffer();
		line.append(row["file"]).append("|")
		.append(row["groupid"]).append("|")
		.append(row["ARTIFACTID"]).append("|")
		.append(row["vERSION"]).append("|")
		.append(row["scope"]).append("|")

		if (set.contains(row["ARTIFACTID"])){
			line.append("N")
		} else {
			line.append("Y")
		}
		bw.write(line.toString());
		bw.newLine();
	}
}

if (args[1]=="R"){
	report();
}
sqlCon.close();
bw.close();