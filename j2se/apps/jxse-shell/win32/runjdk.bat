@echo off
set CLASSPATH=.;..\lib\jxtashell.jar;..\lib\jxta.jar;..\lib\bcprov-jdk14.jar;..\lib\org.mortbay.jetty.jar;..\lib\javax.servlet.jar
java net.jxta.impl.peergroup.Boot
