#! /usr/bin/env groovy

println "Check wandisco-svn.repo ...."
new File("/etc/yum.repos.d/wandisco-svn.repo").createNewFile();
def result = new FileOutputStream(new File("/etc/yum.repos.d/wandisco-svn.repo"));
def bw = new BufferedWriter(new OutputStreamWriter(result,"UTF-8"));
bw.newLine();
bw.write("[WandiscoSVN]")
bw.newLine();
bw.write("name=Wandisco SVN Repo")
bw.newLine();
bw.write("baseurl=http://opensource.wandisco.com/centos/6/svn-1.8/RPMS/$basearch/")
bw.newLine();
bw.write("enabled=1")
bw.newLine();
bw.write("gpgcheck=0")
bw.newLine();
