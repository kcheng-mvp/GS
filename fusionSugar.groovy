#! /usr/bin/env groovy

@Grab(group='jaxen', module='jaxen', version='1.1.4')

import groovy.sql.Sql;
import java.net.InetAddress;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.DefaultLogger;
import java.text.SimpleDateFormat;
import org.jaxen.dom.DOMXPath;
import javax.xml.parsers.DocumentBuilderFactory;



// Instance
def env = System.getenv();
String ADE_VIEW_ROOT = env['ADE_VIEW_ROOT']
String MW_HOME_STANDALONE = env['MW_HOME_STANDALONE'];
String JDEV_HOME = env['JDEV_HOME'];
String MW_HOME = env['MW_HOME'];
def home= new File(System.getProperty("user.home"));
def fusion_apps_wls = new File("${home}/fusion_apps_wls.properties");
def script = null;
def dburl = null;
def domainName =null;
def fusionDbHost = null;
def fusionDbPort = null;
def fusionDbSid = null;
def fusionDbPassword = "fusion";
def inputs = null;
def listenPort = 0000;
def elapsed =  System.currentTimeMillis();
def console = System.console();
def FAM_LIST=["fin","prc","scm"] as ArrayList;

def choice = console.readLine('''** ** ** ** ** ** ** ** ** ** ** ** ** 
1: Create Domain
2: Generate deploy_config.xml
4: Generate **ApplCore_Step_1** deploy_config.xml (You need ApplCore when test DFF)
5: Generate **ApplCore_Step_2** Jars (You need ApplCore when test DFF)
6: Deploy/Undeploy Application
7: fprPatch
8: Full Build/Make Ear
9: overwriteJars
10: Accounting
11: antPatch
12: incrementalBuild
:''');


println "Checking you are in a view or not ......";
println "ADE_VIEW_ROOT ${ADE_VIEW_ROOT}"
if(!ADE_VIEW_ROOT || !MW_HOME_STANDALONE){
        println "Error: You are not in a view, please run 'ade useview' command first !";
        return -1;
}

// common : get DB from fusion_apps_wls.properties
def getDefaultDB = {
//fusionDbHost=slc01bmz.us.oracle.com
//fusionDbPort=1522
//fusionDbSid=slc01bmz

	def processed = false;
    fusion_apps_wls.eachLine{line ->
		if (line.startsWith("dbConnection") && !processed){
			//dbConnection=slc01bpx.us.oracle.com:1522:slc01bpx
			dburl=line.tokenize("=")[1];
			def dbArgs = dburl.tokenize(":");
			fusionDbHost = dbArgs[0];
			fusionDbPort = dbArgs[1]
			fusionDbSid = dbArgs[2];
			processed = true;
		}
		if (line.startsWith("fusionDbHost")){
			fusionDbHost = line.tokenize("=")[1];
		}
		if (line.startsWith("fusionDbPort")){
			fusionDbPort = line.tokenize("=")[1];
		}
		if (line.startsWith("fusionDbSid")){
			fusionDbSid = line.tokenize("=")[1];
		}
		if(line.startsWith("listenPort")){
			listenPort = (line.tokenize("=")[1]).trim();
			println("listenPort is ${listenPort}");
		}
	}
	dburl ="${fusionDbHost}:${fusionDbPort}:${fusionDbSid}";
}

        

// common : check DB is alive or not
def checkDB = {
       def sid = console.readLine("Please enter fusionDbHost sid, Press ENTER for default(${dburl}) : ");
        sid = sid.trim();
        if(!sid){
			println "**************You are using the default database from fusion_apps_wls.properties**************";
			dburl ="${fusionDbHost}:${fusionDbPort}:${fusionDbSid}";
        } else {
			dburl=sid;
			def dbArgs = dburl.tokenize(":");
			fusionDbHost = dbArgs[0];
			fusionDbPort = dbArgs[1]
			fusionDbSid = dbArgs[2];
			//fusionDbSid = sid;
        }
		//fusionDbHost = "${fusionDbSid}.us.oracle.com";
		//fusionDbPort = 1522;
		
        // Check database
        println "Checking database is up or not ......";
        def driver ="oracle.jdbc.OracleDriver";
		
        //def url = "jdbc:oracle:thin:@${fusionDbHost}:${fusionDbPort}:${fusionDbSid}";
		def url = "jdbc:oracle:thin:@${dburl}";
		println "URL : ${url}";
        def sqlCon = Sql.newInstance(url,"fusion","fusion",driver);
        def rs = sqlCon.firstRow("select sysdate from dual");
        if(rs){
            println "DB is alive, sysdate is : ${rs.sysdate}";
            sqlCon.close();
        } else {
			println "The target db is not alive, pleae check your url is correct or not!";
			return -1;
        }
}

// common : Get the target host
def getHost = {
	def addr  = InetAddress.getLocalHost();
	def hostname = addr.getHostName();
	inputs = console.readLine("Please enter the hostname, Press ENTER for default(${hostname}) : ");
	inputs = inputs.trim();
	if(inputs) hostname = inputs;
	return hostname;
}

def createDomain = {
        println "Create Domain ......";
        domainName = console.readLine("Please enter domain name (Press Enter for default) : ");
        def fusion_apps_wls_orig = ("${home}/fusion_apps_wls.properties.orig");
        if(fusionDbHost || domainName){
                fusion_apps_wls.renameTo(fusion_apps_wls_orig);
                def orig = new File("${home}/fusion_apps_wls.properties.orig");
                def fw = new FileWriter(fusion_apps_wls);
                def bw = new BufferedWriter(fw);
                orig.eachLine{line ->
                        def item = line;
                        /*if (fusionDbHost && line.startsWith("dbConnection")) {
                        	item = "dbConnection=${fusionDbHost}.us.oracle.com:${fusionDbPort}:${fusionDbSid}";
                        } else */
						if (domainName && line.startsWith("domainName")){
                        	item = "domainName=${domainName}";
                            
                        }
                        bw.write(item);
                        bw.newLine();
                }
                bw.close();
        }
        // Call script to create Domain
        def wlsconfig= "${ADE_VIEW_ROOT}/fatools/tools/wlsconfig/wlsconfig.sh";
        def processBuilder = new ProcessBuilder(wlsconfig);
        def process = processBuilder.redirectErrorStream(true).start();
        process.inputStream.eachLine {println it}
        processBuilder.redirectErrorStream(false);
        process.waitFor();
        if (process.exitValue()){
                println "Create domain successfully ......";
        }

}

private File generateExecutable(String prefix, String script){
	def executable = File.createTempFile(prefix,".csh");
	executable.deleteOnExit();
	executable.write(script);
	def chmod ="chmod 777 ${executable.getAbsolutePath()}";
	chmod.execute().text;
	return executable;
}

def deployConfig = {
        println "Create deploy_config.xml ......";
		def hostname = getHost();
		def fam = console.readLine("Please enter family ${FAM_LIST} : ");
		if (!FAM_LIST.any{it.equals(fam)}){
			println "Please input the correct family ${FAM_LIST}";
			return -1;
		}
		//fam = "fin".equalsIgnoreCase(fam) ? "fin" : "prc";
        //script ="java -classpath $ADE_VIEW_ROOT/fabuildtools/lib/customtasks.jar:$MW_HOME/oracle_common/modules/oracle.xdk_11.1.0/xmlparserv2.jar:$JDEV_HOME/ant/lib/ant.jar:$JDEV_HOME/jdev/extensions/oracle.odi.navigator/lib/ojdbc5.jar:$MW_HOME/oracle_common/modules/oracle.jps_11.1.1/jps-internal.jar:$MW_HOME/oracle_common/modules/oracle.jps_11.1.1/jps-api.jar:$MW_HOME/wlserver_10.3/server/lib/weblogic.jar oracle.anttasks.deploy.GenerateEnvXML mappingxml=$ADE_VIEW_ROOT/fabuildtools/lib/mapping.xml envtype=others dburl=${dburl} weblogicdomainurl1=http://${hostname}.us.oracle.com:${listenPort}/console wlsosuser1=weblogic wlsospassword1=weblogic1 productfamily=fin weblogicadminuser=weblogic weblogicadminpass=weblogic1 outfile=$ADE_VIEW_ROOT/fusionapps/fin/deploy_config.xml";
		script ="java -classpath $ADE_VIEW_ROOT/fabuildtools/lib/customtasks.jar:$MW_HOME/oracle_common/modules/oracle.xdk_11.1.0/xmlparserv2.jar:$JDEV_HOME/ant/lib/ant.jar:$JDEV_HOME/jdev/extensions/oracle.odi.navigator/lib/ojdbc5.jar:$MW_HOME/oracle_common/modules/oracle.jps_11.1.1/jps-internal.jar:$MW_HOME/oracle_common/modules/oracle.jps_11.1.1/jps-api.jar:$MW_HOME/wlserver_10.3/server/lib/weblogic.jar oracle.anttasks.deploy.GenerateEnvXML mappingxml=$ADE_VIEW_ROOT/fabuildtools/lib/mapping.xml envtype=others dburl=${dburl} weblogicdomainurl1=http://${hostname}.us.oracle.com:${listenPort}/console wlsosuser1=weblogic wlsospassword1=weblogic1 productfamily=${fam} weblogicadminuser=weblogic weblogicadminpass=weblogic1 outfile=$ADE_VIEW_ROOT/fusionapps/${fam}/deploy_config.xml";
        println script;
		def executable = this.generateExecutable("deploy_config", script);
        def process = new ProcessBuilder(executable.getAbsolutePath()).redirectErrorStream(true).start();
        process.inputStream.eachLine {println it};
        def config = new File("${ADE_VIEW_ROOT}/fusionapps/${fam}/deploy_config.xml");
		println "Hello...";
		//def dffEnabled = console.readLine("Are you going to enable DFF ?(Y/*N*): ");
		
        if(config.exists()){
                println "Set essServer ......";
                def tempFile = new File("${ADE_VIEW_ROOT}/fusionapps/${fam}/deploy_config.xml.bak");
                def fw = new FileWriter(tempFile);
                def bw = new BufferedWriter(fw);
				def meet= false;
                config.eachLine{line ->
                        if(line.contains("EarFinancialsEss")){
                            bw.write('''            <J2EEApplication earProfile="EarFinancialsEss" targetserver="ess_server1" domainrefid="1">''');
                        } else if (line.contains("EarProcurementEss")){
							bw.write('''            <J2EEApplication earProfile="EarProcurementEss" targetserver="ess_server1" domainrefid="1"/>''');
						} else if (line.contains("EarScmEss")){
							bw.write('''            <J2EEApplication earProfile="EarScmEss" targetserver="ess_server1" domainrefid="1"/>''');
						}/*
						else if (line.contains("enableGlobalMenu") || line.contains("enableFlex")){
							meet = true;
							bw.write(line);
						}*/
						else {
						/*
							if("Y".equalsIgnoreCase(dffEnabled) && meet) {
								line = line.replace("false","true");
								meet = false;
							}*/
                            bw.write(line);
                        }
                        bw.newLine();
                }
                bw.close();
                tempFile.renameTo(config);
        }
        
}




def applCoreDeployConfig ={
	//https://stbeehive.oracle.com/teamcollab/wiki/Financial+Technology+Team:ApplCore+Setup+deployment
	
	def hostname = getHost();
	script ="java -classpath $ADE_VIEW_ROOT/fabuildtools/lib/customtasks.jar:$MW_HOME/oracle_common/modules/oracle.xdk_11.1.0/xmlparserv2.jar:$JDEV_HOME/ant/lib/ant.jar:$JDEV_HOME/jdev/extensions/oracle.odi.navigator/lib/ojdbc5.jar:$MW_HOME/oracle_common/modules/oracle.jps_11.1.1/jps-internal.jar:$MW_HOME/oracle_common/modules/oracle.jps_11.1.1/jps-api.jar:$MW_HOME/wlserver_10.3/server/lib/weblogic.jar oracle.anttasks.deploy.GenerateEnvXML mappingxml=$ADE_VIEW_ROOT/fabuildtools/lib/mapping.xml envtype=others dburl=${dburl} weblogicdomainurl1=http://${hostname}.us.oracle.com:${listenPort}/console wlsosuser1=weblogic wlsospassword1=weblogic1 productfamily=fnd weblogicadminuser=weblogic weblogicadminpass=weblogic1 outfile=$ADE_VIEW_ROOT/fusionapps/fnd/deploy_config.xml";
	println script;
	
	def executable = this.generateExecutable("AppCore_deploy_config", script);
	def process = new ProcessBuilder(executable.getAbsolutePath()).redirectErrorStream(true).start();
	process.inputStream.eachLine {println it};
	def config = new File("${ADE_VIEW_ROOT}/fusionapps/fnd/deploy_config.xml");
	if(config.exists()){
			println "Set ApplCore Applications ......";
			def tempFile = new File("${ADE_VIEW_ROOT}/fusionapps/fnd/deploy_config.xml.bak");
			def fw = new FileWriter(tempFile);
			def bw = new BufferedWriter(fw);
			def meet= false;
			config.eachLine{line ->
					if(line.contains("<Applications/>")){
						 bw.write('''<Applications>
		<ProductFamily name="fnd">
		<J2EEApplication earProfile="FndSetup" targetserver="AdminServer" domainrefid="1" targetSOACluster="FIN_SOACluster">
		</J2EEApplication>
		</ProductFamily>
	</Applications>''');
					} else if (line.contains("enableGlobalMenu") || line.contains("enableFlex")){
						meet = true;
						bw.write(line);
					}
					else {
						if (meet){
							bw.write("<value>true</value>");
							meet = false;
						} else {
							bw.write(line);
						}
					}
					bw.newLine();
			}
			bw.close();
			tempFile.renameTo(config);
	}
	
}


private Project getAntProject(int level){
	def antProject = new Project();
	def consoleLogger = new DefaultLogger();
	consoleLogger.setErrorPrintStream(System.err);
	consoleLogger.setOutputPrintStream(System.out);
	consoleLogger.setMessageOutputLevel(level);
	antProject.addBuildListener(consoleLogger);
	return antProject;
}

def injectApplCoreJars = {
	
	def antFile = new File("${ADE_VIEW_ROOT}/fusionapps/fnd/fnd-build.xml");
	//def connectionPlan = new File("${ADE_VIEW_ROOT}/fusionapps/fnd/deploy/FndSetup_connectionPlan.xml");
	if(!antFile.exists()){
		println "Can not find file ${ADE_VIEW_ROOT}/fusionapps/fnd/fnd-build.xml/${ADE_VIEW_ROOT}/fusionapps/fnd/deploy/FndSetup_connectionPlan.xml";
		println "Call applCoreDeployConfig().....";
		applCoreDeployConfig();
	}
	def antProject = this.getAntProject(Project.MSG_INFO);
	antProject.fireBuildStarted();
	antProject.init()
	//antProject.init()
	ProjectHelper.projectHelper.configureProject(antProject, antFile);
	println "Execute 'copyFndSetup' targt !";
	antProject.executeTarget('copyFndSetup');
	
	println "Execute 'genericVisitor' targt !";
	antProject.setProperty("visitor","oracle.visitors.InjectAdfModelLibs");
	antProject.setProperty("ear.file","${ADE_VIEW_ROOT}/fusionapps/fnd/deploy/FndSetup.ear");
	antProject.setProperty("Buildfile","${ADE_VIEW_ROOT}/fusionapps/fnd/fnd-build.xml");
	antProject.executeTarget('genericVisitor');
}

def fullBuildEar = {
	def tmp = new File("/tmp");
	tmp.eachFile {
		if((it.getName().startsWith("build_") || it.getName().startsWith("ear_")) && it.getName().endsWith(".log")){
			it.delete();
		}
	}
	script = "ade mkprivate ${ADE_VIEW_ROOT}/fusionapps/com/deploy/atfExternalUriAdf.xml";
	println "${script} ......"
	def executable = this.generateExecutable("mkprivate", script);
	def process = new ProcessBuilder(executable.getAbsolutePath()).redirectErrorStream(true).start();
	process.inputStream.eachLine {println it};
	def antFile = new File("${ADE_VIEW_ROOT}/fusionapps/fin/build-payables.xml");
	def antProject = this.getAntProject(Project.MSG_ERR);
	def fileLogger = new DefaultLogger();
	def sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm");
	def prefix = sdf.format(Calendar.getInstance().getTime());
	def log = new File("/tmp/build_${prefix}.log");
	def ops = new PrintStream(new FileOutputStream(log));
	fileLogger.setErrorPrintStream(ops);
	fileLogger.setOutputPrintStream(ops);
	fileLogger.setMessageOutputLevel(Project.MSG_INFO);
	antProject.addBuildListener(fileLogger);
	antProject.fireBuildStarted();
	antProject.init()
	ProjectHelper.projectHelper.configureProject(antProject, antFile);
	println "**************** full build in process, please dont close the console..... ";
	println "**************** full build log file: /tmp/build_${prefix}.log ";
	antProject.executeTarget("build");
	antProject.fireBuildFinished(null);
	println "**************** full build elapsed : ${(System.currentTimeMillis() - elapsed)/60000} Mins "
	
	
	
	antProject = this.getAntProject(Project.MSG_ERR);
	fileLogger = new DefaultLogger();
	prefix = sdf.format(Calendar.getInstance().getTime());
	log = new File("/tmp/ear_${prefix}.log")
	ops = new PrintStream(new FileOutputStream(log));
	fileLogger.setErrorPrintStream(ops);
	fileLogger.setOutputPrintStream(ops);
	fileLogger.setMessageOutputLevel(Project.MSG_INFO);
	antProject.addBuildListener(fileLogger);
	antProject.fireBuildStarted();
	antProject.init()
	ProjectHelper.projectHelper.configureProject(antProject, antFile);
	println "**************** ear in process, please dont close the console..... ";
	println "**************** ear log file : /tmp/ear_${prefix}.log ";
	antProject.executeTarget("ear");
	antProject.fireBuildFinished(null);
	println "*********ear elapsed : ${(System.currentTimeMillis() - elapsed)/60000} Mins "
	
}



def deployment = {
	def action = console.readLine("Deploy/Undeploy(*D*/U)  : ");
	def target = "j2eeDeploy";
	if(action){
		if ("D".equalsIgnoreCase(action)) target = "j2eeDeploy";
		if("U".equalsIgnoreCase(action)) target = "j2eeUnDeploy";
	}
	def famMap = ['ar':'fin','ap':'fin','gl':'fin','finess':'fin','prc':'prc','log':'scm','appcore':'fnd'];
	def buildMap = ['ar':'build-receivables.xml','ap':'build-payables.xml','gl':'build-ledger.xml','prc':'build-prc.xml','appcore':'fnd-build.xml','finess':'build-financialsEss.xml','log':'build-log.xml'];
	def app = console.readLine("Please enter app names ${famMap} : ");
	app = app.trim().toLowerCase();
	
	def fam = famMap.get(app);
	if(! fam){
		println "Error: Pelase input the correct app name ${famMap} : ";
		return -1;
	}
	def buildFile = buildMap.get(app);

	def antFile = new File("${ADE_VIEW_ROOT}/fusionapps/${fam}/${buildFile}");
	println "${ADE_VIEW_ROOT}/fusionapps/${fam}/${buildFile}";
	def jwsList = getjws(antFile);
	jwsList.each{jws ->
		script = "ade mkprivate ${ADE_VIEW_ROOT}/fusionapps/${fam}/${jws}";
		println "${script} ......"
		def executable = this.generateExecutable("mkprivate", script);
		def process = new ProcessBuilder(executable.getAbsolutePath()).redirectErrorStream(true).start();
		process.inputStream.eachLine {println it};
		
	}
	
	def antProject = this.getAntProject(Project.MSG_INFO);
	antProject.fireBuildStarted();
	antProject.init();
	ProjectHelper.projectHelper.configureProject(antProject, antFile);
	def envFile = "${ADE_VIEW_ROOT}/fusionapps/${fam}/deploy_config.xml"
	println envFile;
	antProject.setProperty("deployenvfile",envFile);
	antProject.executeTarget(target);
}

private List<String> getjws(File buildFile){
	def dbf = DocumentBuilderFactory.newInstance();
	dbf.setValidating(false);
	dbf.setNamespaceAware(true);
	dbf.setFeature("http://xml.org/sax/features/namespaces", false);
	dbf.setFeature("http://xml.org/sax/features/validation", false);
	dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
	dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
	def builder    = dbf.newDocumentBuilder();
	def inputStream = new ByteArrayInputStream(buildFile.getBytes());
	def records     = builder.parse(inputStream).documentElement;
	def targetName = null;
	def props = new HashMap<String,String>();
	new DOMXPath('//property').selectNodes(records).each{it
		def key = new DOMXPath('@name').stringValueOf(it);
		def value = new DOMXPath('@value').stringValueOf(it);
		props.put(key,value);
		//println props;
	}
	def jwsList = new ArrayList<String>();
	new DOMXPath('//target').selectNodes(records).each{it
		targetName = new DOMXPath('@name').stringValueOf(it);
		if("protected_get_workspaces".equalsIgnoreCase(targetName)){
			new DOMXPath('fileset/include').selectNodes(it).each{jws ->
				def njws =  new DOMXPath('@name').stringValueOf(jws);
				//components/${payables.workspace}/${payables.jwsfile}.jws
				//lobalizationSoa/GlobalizationSoa.jws
				def ars = njws.split("components");
				def fjws = (ars.size() > 1? ars[1] : ars[0]).replaceAll('\\$','').replaceAll('\\{','').replaceAll('\\}','');
				//println fjws;
				props.each{k, v ->
					fjws = fjws.replaceAll(k,v); 
				}
				if(fjws.indexOf("/") == 0){
					jwsList.add("components${fjws}");
				} else {
					jwsList.add("components/${fjws}");
				}
			}
			
		}
	}
	return jwsList;
	
}



def fprPatch = {
//	def sid = console.readLine("Please enter target database(Press ENTER for default(${fusionDbSid})): ");
//	sid = sid.trim();
//	sid = sid!=null ? sid : fusionDbSid;
	
	def sid =console.readLine("Please enter target db sid ");
	println "Going to patch data to ${sid} ......";
	def txn = console.readLine("Please enter the txn name(Press Enter for current txn):");
	if(!txn) txn = "currenttransaction";
	script ="fpr -e ${sid} -s ${txn} -p V_CONT_EQUIV=NO";
	println script;
	def executable = this.generateExecutable("fpr", script);
	def process = new ProcessBuilder(executable.getAbsolutePath()).redirectErrorStream(true).start();
	process.inputStream.eachLine {println it};
}


def overwriteJars = {
	def conf = new File("${home}/overwriteJars.properties");
	if(!conf.exists()) conf.createNewFile();
	
	def props = new Properties()
	conf.withInputStream {
	  stream -> props.load(stream)
	}
	
	def tempViewName = props["viewName"];
	def msg = "Please input view name: ";
	if(tempViewName){
		msg = "Please input view name or press enter for default view(xianwche_${tempViewName}):"
	}
	def viewName = console.readLine(msg);
	if(!viewName) viewName = tempViewName;
	conf.write("viewName=${viewName}");
	
	// find the latest changed jars
	def jlib = new File("/net/slc04jwh.us.oracle.com/scratch/xianwche/view_storage/xianwche_${viewName}/fusionapps/fin/components/receivables/jlib");
	println "/net/slc04jwh.us.oracle.com/scratch/xianwche/view_storage/xianwche_${viewName}/fusionapps/fin/components/receivables/jlib";
	def jarList = new ArrayList<File>();
	long checkTime = System.currentTimeMillis() - 5 * 60 * 60*60;
	jlib.eachFileRecurse {
		if (it.isFile() && it.lastModified() >= checkTime) {
			println "****${it.getAbsolutePath()}";
			jarList.add(it);
		}
	}
	
	// other jars
	
	jlib = new File("/net/slc04jwh.us.oracle.com/scratch/xianwche/view_storage/xianwche_${viewName}/fusionapps/jlib");
	println "/net/slc04jwh.us.oracle.com/scratch/xianwche/view_storage/xianwche_${viewName}/fusionapps/jlib";
	checkTime = System.currentTimeMillis() - 5 * 60 * 60*60;
	jlib.eachFileRecurse {
		if (it.isFile() && it.lastModified() >= checkTime) {
			println "****${it.getAbsolutePath()}";
			jarList.add(it);
		}
	}
	
	
	def domainHome = "${MW_HOME_STANDALONE}/user_projects/domains/";
	def folder = new File(domainHome);
	def dmList = new ArrayList();
	folder.eachFile {
		dmList.add(it.getName());
	}
	println "All the domains:  ${dmList}";
	def dm = dmList.get(0);
	if(dmList.size > 1){
		dm = console.readLine("There are more than one domains:${dmList}, please input the target domain:");
		dm = dm.trim();
		if(!dmList.contains(dm)){
			println "Input ${dm} is not a correct domain";
			return -1;
		}
	}
	

	def appHome ="${domainHome}${dm}/servers/AdminServer/upload/ReceivablesApp/app/ReceivablesApp";
	jarList.each { sourceJar ->
		def copied = false;
		(new AntBuilder( )).copy ( file : sourceJar , tofile : new File("/net/slc04jwh.us.oracle.com/scratch/xianwche/share/${sourceJar.name}") ,  overwrite: true, failonerror: true, verbose: true)
		new File(appHome).eachFileRecurse{ targetJar ->
			if(sourceJar.name.equals(targetJar.name)) {
				(new AntBuilder( )).copy ( file : sourceJar , tofile : targetJar ,  overwrite: true, failonerror: true, verbose: true)
			}
		}
	}
}

def accounting = {
	def userDirEss = new File("/tmp/ess/requestFileDirectory");
	if(userDirEss.exists()) new AntBuilder().delete(dir: "/tmp/ess/requestFileDirectory");
	userDirEss.mkdirs();
	println "cp ${ADE_VIEW_ROOT}/fusionapps/fin/fun/noship/essTest/devdb/environment.properties ...."
	def temp = new File("${ADE_VIEW_ROOT}/fusionapps/fin/fun/noship/essTest/devdb/environment.properties");
	def envpro = new File(userDirEss,"environment.properties");
	(new AntBuilder( )).copy ( file : temp , tofile : envpro ,  overwrite: true, failonerror: true, verbose: true);
	//chmod 777 $USER_DIR_ESS/environment.properties
	script = "chmod 777 ${envpro.absolutePath}";
	println script;
	def executable = this.generateExecutable("chmod", script);
	def process = new ProcessBuilder(executable.getAbsolutePath()).redirectErrorStream(true).start();
	process.inputStream.eachLine {println it};
	
	temp = File.createTempFile("environment",".properties");
	temp.deleteOnExit();
	//sed -i "s/###/${TMP_VIEW_ROOT}/g"  $USER_DIR_ESS/environment.properties
	//sed -i "s/@@@/$TMP_USER_DIR_ESS/g" $USER_DIR_ESS/environment.properties
	//sed -i "s/!!!/$db_sid/" $USER_DIR_ESS/environment.properties
	temp.withWriter {w ->
		envpro.eachLine{ line ->
			def t = line.replaceAll("###", ADE_VIEW_ROOT);
			t = t.replaceAll("@@@","/tmp");
			t = t.replaceAll("!!!",fusionDbSid);
			w << t;
			w.newLine();
		}
	}
	temp.renameTo(envpro);
	def tnsAdmin = new File(userDirEss,"tns_admin");
	tnsAdmin.mkdirs();
	//cp $ADE_VIEW_ROOT/fusionapps/fin/fun/noship/essTest/devdb/tns_admin/tnsnames.ora $USER_DIR_ESS/$TNS_ADMIN
	temp = new File("${ADE_VIEW_ROOT}/fusionapps/fin/fun/noship/essTest/devdb/tns_admin/tnsnames.ora");
	def ora = new File(tnsAdmin,"tnsnames.ora");
	(new AntBuilder( )).copy ( file : temp , tofile : ora,  overwrite: true, failonerror: true, verbose: true);
	//chmod 755 $USER_DIR_ESS/$TNS_ADMIN/tnsnames.ora
	script = "chmod 777 ${ora.absolutePath}";
	executable = this.generateExecutable("chmod", script);
	process = new ProcessBuilder(executable.getAbsolutePath()).redirectErrorStream(true).start();
	process.inputStream.eachLine {println it};
	
	temp = File.createTempFile("tnsnames",".ora");
	temp.deleteOnExit();
	temp.withWriter {w ->
		ora.eachLine{ line ->
			def t = line.replaceAll("!!!", fusionDbSid);
			t = t.replaceAll("~~~",fusionDbPort.toString());
			t = t.replaceAll("%%%",fusionDbHost);
			w << t;
			w.newLine();
		}
	}
	temp.renameTo(ora);
	
	def wallet = new File(tnsAdmin,"wallet");
	script = "echo -e 1234'\n'1234 | /usr/local/packages/oracleserver_remote/10.2.0.4/bin/mkstore -wrl ${wallet.absolutePath} -create > /dev/null";
	executable = this.generateExecutable("chmod", script);
	process = new ProcessBuilder(executable.getAbsolutePath()).redirectErrorStream(true).start();
	process.inputStream.eachLine {println it};
	script="echo 1234 | /usr/local/packages/oracleserver_remote/10.2.0.4/bin/mkstore -wrl ${wallet.absolutePath} -createCredential ${fusionDbSid} fusion fusion > /dev/null";
	executable = this.generateExecutable("chmod", script);
	process = new ProcessBuilder(executable.getAbsolutePath()).redirectErrorStream(true).start();
	process.inputStream.eachLine {println it};
	//chmod 755 "$USER_DIR_ESS/$TNS_ADMIN/wallet"
	script ="chmod 755 ${wallet.absolutePath}"
	executable = this.generateExecutable("chmod", script);
	process = new ProcessBuilder(executable.getAbsolutePath()).redirectErrorStream(true).start();
	process.inputStream.eachLine {println it};
	//chmod 744 "$USER_DIR_ESS/$TNS_ADMIN/wallet/cwallet.sso"
	script ="chmod 744 ${wallet.absolutePath}/cwallet.sso";
	executable = this.generateExecutable("chmod", script);
	process = new ProcessBuilder(executable.getAbsolutePath()).redirectErrorStream(true).start();
	process.inputStream.eachLine {println it};
	//create sqlnet.ora file
	//cp $ADE_VIEW_ROOT/fusionapps/fin/fun/noship/essTest/devdb/tns_admin/sqlnet.ora $USER_DIR_ESS/$TNS_ADMIN
	temp = new File("${ADE_VIEW_ROOT}/fusionapps/fin/fun/noship/essTest/devdb/tns_admin/sqlnet.ora");
	def sqlnet = new File(tnsAdmin,"sqlnet.ora");
	(new AntBuilder( )).copy ( file : temp , tofile : sqlnet,  overwrite: true, failonerror: true, verbose: true);
	//chmod 755 $USER_DIR_ESS/$TNS_ADMIN/sqlnet.ora
	script ="chmod 755 ${sqlnet.absolutePath}";
	executable = this.generateExecutable("chmod", script);
	process = new ProcessBuilder(executable.getAbsolutePath()).redirectErrorStream(true).start();
	process.inputStream.eachLine {println it};
	//sed -i "s/@@@/\/tmp/g" $USER_DIR_ESS/$TNS_ADMIN/sqlnet.ora
	temp = File.createTempFile("sqlnet",".ora");
	temp.deleteOnExit();
	temp.withWriter {w ->
		sqlnet.eachLine{ line ->
			w << line.replaceAll("@@@", "/tmp");
			w.newLine();
		}
	}
	temp.renameTo(sqlnet);
	//# create directory for c executables
	def cbin = new File(userDirEss,"bin");
	cbin.mkdir();
	//cp $VIEW_ROOT/fusionapps/bin/linuxx64/* ./bin/;
	script ="cp ${ADE_VIEW_ROOT}/fusionapps/bin/linuxx64/* ${cbin.absolutePath}";
	executable = this.generateExecutable("chmod", script);
	process = new ProcessBuilder(executable.getAbsolutePath()).redirectErrorStream(true).start();
	process.inputStream.eachLine {println it};
    //chmod 711 ./bin/*
	script ="chmod 711 ${cbin.absolutePath}/*";
	println script;
	executable = this.generateExecutable("chmod", script);
	process = new ProcessBuilder(executable.getAbsolutePath()).redirectErrorStream(true).start();
	process.inputStream.eachLine {println it};
	// Verify DB connection
	//setenv "TNS_ADMIN ${tnsAdmin.absolutePath}"
	//in the .cshrc
	//sqlplus fusion/fusion@slc05icc < /dev/null
	script = "sqlplus fusion/fusion@${fusionDbSid} < /dev/null"
	executable = this.generateExecutable("chmod", script);
	process = new ProcessBuilder(executable.getAbsolutePath()).redirectErrorStream(true).start();
	def connected = false;
	process.inputStream.eachLine {
		if(it.indexOf("Connected to:") > -1) connected = true;
	};
	if(connected){
		println "***********************************************************************  Success **************************************************************"
		println "**********Overwrite DOMAIN_HOME /config/fmwconfig/environment.properties with /tmp/ess/requestFileDirectory/environment.properties **********"
		println "***********************************************************************************************************************************************"
	} else {
		println "Error : Can't connect to the db with ${tnsAdmin.absolutePath}";
	}
}

def antPatch = {
	script = "ade expand ${ADE_VIEW_ROOT}/fabuildtools/*";
	println script;
	def executable = this.generateExecutable("expand", script);
	def process = new ProcessBuilder(executable.getAbsolutePath()).redirectErrorStream(true).start();
	process.inputStream.eachLine {println it};
	def txn = null;
	def bug = null;
	script = "ade describetrans";
	executable = this.generateExecutable("desc", script);
	process = new ProcessBuilder(executable.getAbsolutePath()).redirectErrorStream(true).start();
	process.inputStream.eachLine {
		if(it.startsWith("TRANSACTION:")){
			txn = ((it.split(":"))[1]).trim();
			bug = ((txn.split("-"))[1]).trim();
			println "Txn : ${txn}, bug : ${bug}";
		}
	};
	script = "ant patch -f  ${ADE_VIEW_ROOT}/fabuildtools/lib/build-patch.xml -DgraphFile=${ADE_VIEW_ROOT}/fusionapps/build_metadata/graph.xml  -Dtransaction=${txn} -Dbug=${bug} -Dbaseproductfamily=fin  -Dmode=dev "
	println script;
	def log = new File("${home}/temp/antpatch-${txn}.log");
	def bw = new BufferedWriter(new FileWriter(log));
	//> ~/temp/antpatch-${bug}.log &
	executable = this.generateExecutable("antPatch", script);
	process = new ProcessBuilder(executable.getAbsolutePath()).redirectErrorStream(true).start();
	process.inputStream.eachLine {
		println it
		bw.write(it);
		bw.newLine();
	};
	bw.close();
	println "************Done************";
	println "Log : ${log.absolutePath}"
}

def incrBuild = {
	def txn = null;
	script = "ade describetrans";
	executable = this.generateExecutable("desc", script);
	process = new ProcessBuilder(executable.getAbsolutePath()).redirectErrorStream(true).start();
	process.inputStream.eachLine {
		if(it.startsWith("TRANSACTION:")){
			txn = ((it.split(":"))[1]).trim();
		}
	};
	// ant -f build.xml incrementalBuild -Dtransactions=xianwche_bug-18776661
	script = "ant -f  ${ADE_VIEW_ROOT}/fusionapps/fin/build.xml incrementalBuild -Dtransactions=${txn}"
	println script;
	def log = new File("${home}/temp/incr-${txn}.log");
	def bw = new BufferedWriter(new FileWriter(log));
	//> ~/temp/antpatch-${bug}.log &
	executable = this.generateExecutable("incrBuild", script);
	process = new ProcessBuilder(executable.getAbsolutePath()).redirectErrorStream(true).start();
	process.inputStream.eachLine {
		println it
		bw.write(it);
		bw.newLine();
	};
	bw.close();
	println "************Done************";
	println "Log : ${log.absolutePath}"
}




if(choice == "1"){
	getDefaultDB();
	checkDB();
	createDomain();
} else if (choice =="2") {
	getDefaultDB();
	checkDB();
    deployConfig();
} else if(choice =="3"){
	//setupAttachmentEnv();
} else if (choice =="4"){
	getDefaultDB();
	checkDB();
	applCoreDeployConfig();
} else if (choice =="5"){
	injectApplCoreJars();
} else if (choice =="6"){
	deployment();
} else if (choice=="7"){
	getDefaultDB();
	checkDB();
	fprPatch();
} else if(choice =="8"){
	fullBuildEar();
}  else if (choice=="9"){
	overwriteJars();
}
else if (choice=="10"){
	getDefaultDB();
	checkDB();
	accounting();
}else if (choice=="11"){
	getDefaultDB();
	checkDB();
	antPatch();
} else if (choice =="12"){
	incrBuild();
}
println "Elapsed : ${(System.currentTimeMillis() - elapsed)/60000} Mins";
if(choice == "6"){
	
	println "AppCore : http://${}:${}/fndSetup/faces/SetupDemo_UIShellPage (application_developer/Welcome1)"
}
