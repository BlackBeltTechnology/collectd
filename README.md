# collectd
Collectd modules for Java applications

## Contents

* collectd-core: core module that implements collectd sender using UDP protocol
* collectd-osgi: OSGi bundle exposing declarative service and also contains OSGi event handler
* collectd-commands: Apache Karaf commands to send metrics from console
* collectd-feature: Apache Karaf feature describing dependencies and support deployment
* collectd-jmx: JMX metrics provider for collectd
* collectd-jmx-agent: Java agent sending JMX metrics of JVM defined by XML

## How to build

Use the following command to build and sign (including JavaDoc and source JARs) modules.

~~~~
mvn clean install -Pbuild-extras -Psign
~~~~

Add `deploy` argument to upload artifacts to Sonatype OSS Repository.

## How to use

### Apache Karaf

Install as Apache Karaf feature (Apache Felix SCR and Jasypt encryption dependencies are installed too).

~~~~
feature:repo-add mvn:hu.blackbelt/collectd-karaf-feature/1.0.1/xml/karaf4-features
feature:install collectd
~~~~

