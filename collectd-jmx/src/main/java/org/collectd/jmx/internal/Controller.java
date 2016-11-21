package org.collectd.jmx.internal;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.collectd.config.CollectdConstants;
import org.collectd.jmx.services.Collector;
import org.collectd.jmx.xml.ns.definition.Jmx;

@Slf4j
public class Controller {

    private static final String COLLECTOR_THREAD_NAME = "collectd";
    public static final long DEFAULT_SCHEDULER_INTERVAL = 1000L;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, new CollectorThreadFactory());

    public Controller(final Collection<String> configFiles) {
        this(configFiles, DEFAULT_SCHEDULER_INTERVAL);
    }

    public Controller(final Collection<String> configFiles, final long interval) {
        Objects.requireNonNull(configFiles, "No JMX configuration file is specified");
        if (log.isDebugEnabled()) {
            log.debug("JMX collector configuration files: " + configFiles);
        }

        final ConfigurationLoader configLoader = new ConfigurationLoader();
        final Collection<Jmx> configs = new LinkedList<>();
        for (final String configFile : configFiles) {
            configs.add(configLoader.loadValidators(new File(configFile.trim())));
        }
        
        final Config config = Config.initFromCommandLine();

        final Collector collector = new Collector(config, configs);
        scheduler.scheduleAtFixedRate(collector, 0, config.getInterval(), TimeUnit.MILLISECONDS);
    }
    
    public void shutdown() {
        scheduler.shutdownNow();
    }

    private static class CollectorThreadFactory implements ThreadFactory {

        @Override
        public Thread newThread(Runnable task) {
            final Thread thread = new Thread(task);
            thread.setName(COLLECTOR_THREAD_NAME);
            thread.setDaemon(true);
            return thread;
        }
    }

    @lombok.Getter
    public static class Config {

        public static final String HOST_ARGUMENT = "collectd.host";
        private String host = CollectdConstants.DEFAULT_IPV4_ADDRESS;
        
        public static final String PORT_ARGUMENT = "collectd.port";
        private int port = CollectdConstants.DEFAULT_UDP_PORT;
        
        public static final String PACKET_SIZE_ARGUMENT = "collectd.packetSize";
        private int packetSize = CollectdConstants.DEFAULT_PACKET_SIZE;

        public static final String INTERVAL_ARGUMENT = "collectd.interval";
        private long interval = DEFAULT_SCHEDULER_INTERVAL;
        
        public static final String INSTANCE_ARGUMENT = "collectd.instance";
        private String instance;
        
        public static final String JMX_URL_ARGUMENT = "collectd.jmxUrl";
        private String jmxUrl;
        
        public static final String CLIENT_ARGUMENT = "collectd.client";
        private String client;
        
        static Config initFromCommandLine() {
            final Config config = new Config();
            
            config.setHost(System.getProperty(HOST_ARGUMENT));
            config.setPort(System.getProperty(PORT_ARGUMENT));
            config.setPacketSize(System.getProperty(PACKET_SIZE_ARGUMENT));
            config.setInterval(System.getProperty(INTERVAL_ARGUMENT));
            config.setInstance(System.getProperty(INSTANCE_ARGUMENT));
            config.setJmxUrl(System.getProperty(JMX_URL_ARGUMENT));
            config.setClient(System.getProperty(CLIENT_ARGUMENT));
            
            return config;
        }

        void setHost(final String host) {
            if (host != null) {
                this.host = host;
            }
        }

        void setPort(final String port) {
            if (port != null) {
                this.port = Integer.parseInt(port);
            }
        }

        void setPacketSize(final String packetSize) {
            if (packetSize != null) {
                this.packetSize = Integer.parseInt(packetSize);
            }
        }

        void setInterval(final String interval) {
            if (interval != null) {
                this.interval = Long.parseLong(interval);
            }
        }

        void setInstance(final String instance) {
            this.instance = instance;
        }

        public void setJmxUrl(final String jmxUrl) {
            if (jmxUrl != null) {
                this.jmxUrl = jmxUrl;
            }
        }

        public void setClient(final String client) {
            if (client != null) {
                this.client = client;
            }
        }
    }
}
