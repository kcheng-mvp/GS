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

def ivyXml = new XmlParser().parse(ivy)



def groupId = ivyXml.info[0].attribute("organisation");

def artifactId = ivyXml.publications[0].artifact[0].attribute("name");

def packaging = ivyXml.publications[0].artifact[0].attribute("type");




def scriptDir = new File(getClass().protectionDomain.codeSource.location.path);
def pomXml = new File(scriptDir.parentFile, "pomTemp.xml").getText();
pomXml = pomXml.replace("#groupId#",groupId).replace("#artifactId#",artifactId).replace("#packaging#",packaging).replace("#version#",version)



def pom = new XmlParser().parseText(pomXml)



def dependencies = pom.'dependencies'

def renameMap = ['acegi-security': 'org.acegisecurity', 
'acegi-security-cas': 'org.acegisecurity',
'acegi-security-cas': 'org.acegisecurity',
'c3p0': 'c3p0',
'cglib-node':'cglib',
'commons-beanutils':'commons-beanutils',
'commons-betwixt':'commons-betwixt',
'commons-collections':'commons-collections',
'commons-configuration':'commons-configuration',
'commons-dbcp':'commons-dbcp',
'commons-digester':'commons-digester',
'commons-io':'commons-io',
'commons-lang':'commons-lang',
'commons-logging':'commons-logging',
'commons-net':'commons-net',
'commons-pool':'commons-pool',
'commons-fileupload':'commons-fileupload',
'ehcache':'net.sf.ehcache',
'ezmorph':'net.sf.ezmorph',
'freemarker':'org.freemarker',
'hibernate ':'org.hibernate',


  ]


ivyXml.dependencies[0].dependency.each{

	def name =it.attribute("name");
	def org = it.attribute("org") ?  it.attribute("org") : groupId;
	org = renameMap.get(name) ? renameMap.get(name) : name;
	def rev = it.attribute("rev");

	def node = dependencies*.appendNode('dependency')
	node*.appendNode('groupId',org)
	node*.appendNode('artifactId',name)
	node*.appendNode('version',rev)
}



new File("pom.xml").withWriter{ out ->
  printer = new XmlNodePrinter( new PrintWriter(out) )
  printer.preserveWhitespace = true
  printer.print(pom)
}


