#! /bin/sh

if [ -f setconf.sh ]; then
    sh setconf.sh
    cat PlatformPeerGroup > ../lib/PlatformPeerGroup
fi

if [ -f ../jre/bin/java ]; then
    JAVA="../jre/bin/java"
else
    JAVA="java"
fi

$JAVA -classpath ../lib/jxtashell.jar:../lib/jxta.jar:../lib/bcprov-jdk14.jar:../lib/org.mortbay.jetty.jar:../lib/javax.servlet.jar net.jxta.impl.peergroup.Boot

