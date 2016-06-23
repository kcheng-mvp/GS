#! /usr/bin/env groovy

def SKIP_BREW_DOCTOR=0
def RUBY_VERSION="2.0.0-p451"
def NODE_VERSION="0.10.28"
def USER_HOME=System.getProperty("user.home");


def script="Using Ruby ${RUBY_VERSION}, node ${NODE_VERSION}";
def installed = false;
def console = System.console();

def homebrew = {
	script ="brew";
	def executable = this.generateExecutable("homebrew", script);
	def process = new ProcessBuilder(executable.getAbsolutePath()).redirectErrorStream(true).start();
	process.inputStream.eachLine {
		if(it.indexOf("brew uninstall") > -1){
			installed = true;
			println "Skip Homebrew installation ...... "
		}
	}; 
	if(!installed){
		script ='ruby -e "$(curl -fsSL https://raw.github.com/Homebrew/homebrew/go/install)"';
		println "MSG : ${script}"
		executable = this.generateExecutable("homebrew", script);
		process = new ProcessBuilder(executable.getAbsolutePath()).redirectErrorStream(true).start();
	}
}

def git ={
	installed = false;
	script ="git --version";
	def executable = this.generateExecutable("git", script);
	def process = new ProcessBuilder(executable.getAbsolutePath()).redirectErrorStream(true).start();
	process.inputStream.eachLine {
		if(it.indexOf("git version") > -1){
			installed = true;
			println "Skip git installation ...... "
		}
	}; 
	if(!installed){
		script ="brew install git"
		executable = this.generateExecutable("git", script);
		process = new ProcessBuilder(executable.getAbsolutePath()).redirectErrorStream(true).start();
		process.inputStream.eachLine { println it}
	}
}

def rbenv = {
		//~/.rbenv
	//~/.rbenv/plugins/ruby-build
	installed = false;
	def rbenv = "${USER_HOME}/.rbenv";
	def plugins = "${rbenv}/plugins/ruby-build";
	def rbenv_f = new File(rbenv);
	def plugins_f = new File(plugins)
	if(!rbenv_f.exists()){
		script="git clone https://github.com/sstephenson/rbenv.git ${rbenv}"
		println "MSG : ${script}";
		executable = this.generateExecutable("rbenv", script);
		process = new ProcessBuilder(executable.getAbsolutePath()).redirectErrorStream(true).start();
		process.inputStream.eachLine { println it}
		script="git clone https://github.com/sstephenson/ruby-build.git ${plugins}";
		println "MSG : ${script}";
		executable = this.generateExecutable("plugins", script);
		process = new ProcessBuilder(executable.getAbsolutePath()).redirectErrorStream(true).start();
		process.inputStream.eachLine { println it}
	} else if (!plugins_f.exists()){
		script="git clone https://github.com/sstephenson/ruby-build.git ${plugins}";
		println "MSG : ${script}";
		executable = this.generateExecutable("plugins", script);
		process = new ProcessBuilder(executable.getAbsolutePath()).redirectErrorStream(true).start();
		process.inputStream.eachLine { println it}
	}
	//PATH="~/.rbenv/bin:~/.ndenv/bin:$SCRIPT_HOME:$PATH"
	def bash_profile = new File("${USER_HOME}/.bash_profile");
	def bash_profile_back = new File("${USER_HOME}/.bash_profile.old");
	bash_profile.renameTo(bash_profile_back);
	def fw = new FileWriter(bash_profile);
    def bw = new BufferedWriter(fw);
    def rbenvInit = false;
	bash_profile_back.eachLine{
		def temp = it;
		if(it.indexOf("PATH=") > -1 && it.indexOf("\$GS") > -1 && it.indexOf(".rbenv") < 0){
			temp = it.replace("PATH=","PATH=\$HOME/.rbenv/bin:");
		} 
		bw.write(temp);
		bw.newLine();
		if(it.indexOf("rbenv init -)") > -1) rbenvInit = true;
	}
	//echo 'eval "$(rbenv init -)"' >> ~/.bash_profile
	if(!rbenvInit) bw.write('eval "$(rbenv init -)');
	bw.close();

	// effective the current changes
	installed = false;
	executable = this.generateExecutable("plugins", "source ~/.bash_profile");
	process = new ProcessBuilder(executable.getAbsolutePath()).redirectErrorStream(true).start();
	process.inputStream.eachLine { println it}
	executable = this.generateExecutable("checker","rbenv versions");
	process = new ProcessBuilder(executable.getAbsolutePath()).redirectErrorStream(true).start();
	process.inputStream.eachLine { 
		if(it.indexOf(RUBY_VERSION) > -1){
			installed = true;
			println "Skip rbenv installation ...... "
		}
	}
	if(!installed){
		// install target ruby
		println "MSG : rbenv install ${RUBY_VERSION} ..."
		executable = this.generateExecutable("rbevn", "rbenv install ${RUBY_VERSION}");
		process = new ProcessBuilder(executable.getAbsolutePath()).redirectErrorStream(true).start();
		process.inputStream.eachLine { println it}
		// rehash rbenv
		println "MSG : rbenv rehash ..."
		executable = this.generateExecutable("rbevn", "rbenv rehash");
		process = new ProcessBuilder(executable.getAbsolutePath()).redirectErrorStream(true).start();
		process.inputStream.eachLine { println it}

	}
	

}

// Node

def ndevn = {
	//git clone https://github.com/riywo/ndenv ~/.ndenv
 
	//git clone https://github.com/riywo/node-build.git ~/.ndenv/plugins/node-build 
	def ndenv= "${USER_HOME}/.ndenv";
	def plugins = "${ndenv}/plugins/node-build";
	if(!(new File(ndenv).exists())){
		script="git clone https://github.com/riywo/ndenv ${ndenv}"
		println "MSG: ${script}";
		executable = this.generateExecutable("ndenv", script);
		process = new ProcessBuilder(executable.getAbsolutePath()).redirectErrorStream(true).start();
		process.inputStream.eachLine { println it}

		script="git clone https://github.com/riywo/node-build.git ${plugins}"
		println "MSG: ${script}";
		executable = this.generateExecutable("ndenv", script);
		process = new ProcessBuilder(executable.getAbsolutePath()).redirectErrorStream(true).start();
		process.inputStream.eachLine { println it}
	}

	def bash_profile = new File("${USER_HOME}/.bash_profile");
	def bash_profile_back = new File("${USER_HOME}/.bash_profile.old");
	bash_profile.renameTo(bash_profile_back);
	def fw = new FileWriter(bash_profile);
    def bw = new BufferedWriter(fw);
    def rbenvInit = false;
	bash_profile_back.eachLine{
		def pos = it.indexOf('PATH="');
		def temp = it;
		if(it.indexOf("PATH=") > -1 && it.indexOf("\$GS") > -1 && it.indexOf(".ndenv") < 0){
			temp = it.replace("PATH=","PATH=\$HOME/.ndenv/bin:");
		} 
		bw.write(temp);
		bw.newLine();
		if(it.indexOf("ndenv init -)") > -1) rbenvInit = true;
	}
	if(!rbenvInit) bw.write('eval "$(ndenv init -)');
	bw.close();

	// effective the current changes
	executable = this.generateExecutable("plugins", "source ~/.bash_profile");
	process = new ProcessBuilder(executable.getAbsolutePath()).redirectErrorStream(true).start();
	process.inputStream.eachLine { println it}

	installed = false;
	executable = this.generateExecutable("plugins", "source ~/.bash_profile");
	process = new ProcessBuilder(executable.getAbsolutePath()).redirectErrorStream(true).start();
	process.inputStream.eachLine { println it}
	executable = this.generateExecutable("checker","ndenv versions");
	process = new ProcessBuilder(executable.getAbsolutePath()).redirectErrorStream(true).start();
	process.inputStream.eachLine { 
		if(it.indexOf(NODE_VERSION) > -1){
			installed = true;
			println "Skip ndenv installation ...... "
		}
	}
	if(!installed){
		executable = this.generateExecutable("plugins", "ndenv install ${NODE_VERSION}");
		process = new ProcessBuilder(executable.getAbsolutePath()).redirectErrorStream(true).start();
		process.inputStream.eachLine { println it}

		executable = this.generateExecutable("plugins", "ndenv rehash");
		process = new ProcessBuilder(executable.getAbsolutePath()).redirectErrorStream(true).start();
		process.inputStream.eachLine { println it}
	}




}

def tools = {
	def list = ['ag', 'phantomjs','chromedriver','android-sdk','qt','postgresql','ant'] as List;
	list.each{tool  ->
		script = "brew info ${tool}";
		executable = this.generateExecutable("tools", script);
		process = new ProcessBuilder(executable.getAbsolutePath()).redirectErrorStream(true).start();
		process.inputStream.eachLine {
			if(it.indexOf("Error") > -1){
				println "MSG : brew install ${it}"
				def t = this.generateExecutable("tools", "brew install ${it}");
				def p = new ProcessBuilder(executable.getAbsolutePath()).redirectErrorStream(true).start();
				p.inputStream.eachLine {line ->
					println line;
				}
			}
		}
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

homebrew();
git();
rbenv();
ndevn();
tools();
