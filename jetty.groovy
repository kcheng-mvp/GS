#! /usr/bin/env groovy

@GrabResolver(name='restlet', root='http://repo2.maven.org/maven2/')
@Grab(group='org.eclipse.jetty', module='jetty-webapp', version='9.4.6.v20170531')
@Grab(group='org.eclipse.jetty', module='jetty-server', version='9.4.6.v20170531')
@Grab(group='org.eclipse.jetty', module='jetty-servlet', version='9.4.6.v20170531')
@Grab(group='org.eclipse.jetty', module='apache-jsp', version='9.4.6.v20170531')
@Grab(group='javax.servlet', module='javax.servlet-api', version='3.1.0')
//@GrabExclude('org.eclipse.jetty.orbit:javax.servlet')
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.*


def publishedFolder = args ? args[0] : '.'

def server = new Server(8080)
def context = new ServletContextHandler(server, '/', ServletContextHandler.SESSIONS)
def webappContext = new org.eclipse.jetty.webapp.WebAppContext(publishedFolder, '/sandbox')
context.setHandler(webappContext)
server.start()
println 'Jetty server started. Press Ctrl+C to stop.'