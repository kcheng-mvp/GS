@GrabResolver(name='restlet', root='http://repo2.maven.org/maven2/')
@Grab(group='org.eclipse.jetty', module='jetty-server', version='8.1.10.v20130312')
@Grab(group='org.eclipse.jetty', module='jetty-servlet', version='8.1.10.v20130312')
@Grab(group='javax.servlet', module='javax.servlet-api', version='3.0.1')
@GrabExclude('org.eclipse.jetty.orbit:javax.servlet')
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.*
 
def runWithJetty(servlet, port) {
   def jetty = new Server(port)
   def context = new ServletContextHandler(jetty, '/', ServletContextHandler.SESSIONS);
   context.addServlet(new ServletHolder(servlet), '/*')
   jetty.start()
}