package org.collectd.jmx.agent;

/**
 * Dummy main class for testing.
 */
public class Dummy {

    private static final long TIMEOUT = 60000;

    public static void main(String[] args) throws Exception {
        System.out.println("Usage: java -Dorg.slf4j.simpleLogger.log.org.collectd=TRACE -Dcollectd.host=ryelxdmaoci1.rye.avon.com -Dcollectd.instance=tester -Dcollectd.port=12026 -Dcollectd.packetSize=1452 -Dcollectd.client=testclient -javaagent:target/collectd-jmx-agent-1.0.0-SNAPSHOT-jar-with-dependencies.jar=src/main/config/javalang-collectd.xml -jar target/collectd-jmx-agent-1.0.0-SNAPSHOT-jar-with-dependencies.jar");
        Thread.sleep(TIMEOUT);
    }
}
