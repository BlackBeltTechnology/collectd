package org.collectd.jmx.agent;

import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.collectd.jmx.internal.Controller;

/**
 * Premain class for JMX Java agent.
 */
@Slf4j
public class Main {

    /**
     * Start JMX Java agent.
     *
     * @param args agent arguments
     * @param instr instrumentation
     */
    public static void premain(final String args, final Instrumentation instr) {
        log.info("Initializing JMX Java agent ...");
        if (args != null) {
            final Controller controller = new Controller(Arrays.asList(args.split(",")));

            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    controller.shutdown();
                }
            });
        } else {
            log.warn("Missing JMX configuration, set javaagent argument");
        }
    }
}
