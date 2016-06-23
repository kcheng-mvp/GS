#!/usr/bin/env groovy

@Grapes([
	@Grab(group='jaxen', module='jaxen', version='1.1.4')
])
@GrabExclude('xml-apis:xml-apis')

import javax.xml.parsers.DocumentBuilderFactory;



def ivy = new File("ivy.xml");
def maintenance = new File("maintenance.properties");

if(!ivy.exists() || !maintenance.exists()){
	println "can not find file ivy.xml or maintenance.properties"
	return -1;
}

def props = new Properties();
maintenance.withInputStream { stream ->
	props.load(stream)
}

def version = props['version'];
version = "1.0.0";

def ivyXml = new XmlParser().parse(ivy)



def groupId = ivyXml.info[0].attribute("organisation");
groupId = "com.fangcang"

def artifactId = ivyXml.publications[0].artifact[0].attribute("name");

def packaging = ivyXml.publications[0].artifact[0].attribute("type");




def scriptDir = new File(getClass().protectionDomain.codeSource.location.path);
def pomXml = new File(scriptDir.parentFile, "pomTemp.xml").getText();
pomXml = pomXml.replace("#groupId#",groupId).replace("#artifactId#",artifactId).replace("#packaging#",packaging).replace("#version#",version)



def pom = new XmlParser().parseText(pomXml)



def dependencies = pom.'dependencies'

def renameMap = [
"spring":"org.springframework#spring-core",
"ibatis":"org.mybatis#mybatis",
"httpclient":"org.apache.httpcomponents#httpclient",
"httpcore":"org.apache.httpcomponents#httpcore",
"commons-lang":"commons-lang#commons-lang",
"commons-logging":"commons-logging#commons-logging",
"commons-configuration":"commons-configuration#commons-configuration",
"commons-beanutils":"commons-beanutils#commons-beanutils",
"commons-codec":"commons-codec#commons-codec",
"hessian":"com.caucho#hessian",
"quartz":"org.quartz-scheduler#quartz"
 ]



ivyXml.dependencies[0].dependency.each{
	def scope ="compile"
	def name =it.attribute("name");
	def org = it.attribute("org") ?  it.attribute("org") : groupId;

	def rev = it.attribute("rev");
	def org_name = renameMap.get(name);
	if(org_name){
		def ps = org_name.split("#");
		org= ps[0];
		name= ps[1]
	}

	def node = dependencies*.appendNode('dependency')
	node*.appendNode('groupId',org)
	node*.appendNode('artifactId',name)
	node*.appendNode('version',rev)
	node*.appendNode('scope',scope)
	

}


new File("pom.xml").withWriter{ out ->
  printer = new XmlNodePrinter( new PrintWriter(out) )
  printer.preserveWhitespace = true
  printer.print(pom)
}


