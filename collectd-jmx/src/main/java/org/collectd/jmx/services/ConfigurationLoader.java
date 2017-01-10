package org.collectd.jmx.services;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import lombok.extern.slf4j.Slf4j;
import org.collectd.jmx.xml.ns.definition.Jmx;
import org.xml.sax.SAXException;

/**
 * JMX configuration loader.
 */
@Slf4j
public class ConfigurationLoader {

    private Unmarshaller unmarshaller;

    /**
     * Create new configuration loader instance.
     */
    public ConfigurationLoader() {
        final SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

        try {
            final URL schemaUrl = getClass().getClassLoader().getResource("META-INF/jmx_1.0.xsd");
            final Schema schema = sf.newSchema(schemaUrl);
            final JAXBContext jaxbContext = JAXBContext.newInstance("org.collectd.jmx.xml.ns.definition");

            unmarshaller = jaxbContext.createUnmarshaller();
            unmarshaller.setSchema(schema);
        } catch (JAXBException | SAXException ex) {
            log.error("Unable to initialize configuration loader", ex);
        }
    }

    /**
     * Load business validation rules from the specified file.
     *
     * @param file JMX definition (XML)
     * @return JMX definition object loaded from file
     */
    public Jmx loadValidators(final File file) {
        Objects.requireNonNull(unmarshaller, "JAXB is not initialized yet");

        try {
            return (Jmx) unmarshaller.unmarshal(file);
        } catch (JAXBException ex) {
            throw new IllegalStateException("Unable to load configuration file", ex);
        }
    }
    
    /**
     * Load business validation rules from URL.
     *
     * @param url JMX definition (URL)
     * @return JMX definition object loaded from file
     */
    public Jmx loadValidators(final URL url) {
        Objects.requireNonNull(unmarshaller, "JAXB is not initialized yet");

        try {
            return (Jmx) unmarshaller.unmarshal(url);
        } catch (JAXBException ex) {
            throw new IllegalStateException("Unable to load configuration file", ex);
        }
    }
    
    /**
     * Load business validation rules from input stream.
     *
     * @param input JMX definition (input stream)
     * @return JMX definition object loaded from file
     */
    public Jmx loadValidators(final InputStream input) {
        Objects.requireNonNull(unmarshaller, "JAXB is not initialized yet");

        try {
            return (Jmx) unmarshaller.unmarshal(input);
        } catch (JAXBException ex) {
            throw new IllegalStateException("Unable to load configuration file", ex);
        }
    }
}
