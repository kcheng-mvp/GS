import groovy.sql.Sql;
import groovy.grape.Grape;
import java.util.zip.ZipFile;
import java.sql.Types;
import org.hibernate.dialect.*;

@Grapes([
        @Grab(group='com.h2database', module='h2', version='1.2.141'),
        @GrabConfig(systemClassLoader=true)
])

// Run the server without console.
/*
def h2Server =  org.h2.tools.Server.createTcpServer(args);
h2Server.start();
println "H2 database start successfully ...";
println "Please open the link https://localhost:8082/login.jsp";
*/

// Run the server with console
def h2Console = new org.h2.tools.Console();
h2Console.runTool(args);