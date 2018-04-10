package io.enmasse.example.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Properties;

public class AppCredentials {
    private static final Logger log = LoggerFactory.getLogger(AppCredentials.class);
    private final String hostname;
    private final int port;
    private final String username;
    private final String password;
    private final String x509Certificate;
    private final KeyStore trustStore;

    public AppCredentials(String hostname, int port, String username, String password, String x509Certificate, KeyStore trustStore) {
        this.hostname = hostname;
        this.port = port;
        this.username = username;
        this.password = password;
        this.x509Certificate = x509Certificate;
        this.trustStore = trustStore;
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getX509Certificate() {
        return x509Certificate;
    }

    public KeyStore getTrustStore() {
        return trustStore;
    }

    public static AppCredentials create() throws Exception {
        if (isOnKube()) {
            log.info("Loading configuration from secret");
            return fromSystem();
        } else {
            log.info("Loading configuration from properties");
            return fromProperties();
        }
    }

    public static AppCredentials fromSystem() throws Exception {
        String hostname = readSecretFile("host");
        int port = Integer.parseInt(readSecretFile("port"));
        String username = readSecretFile("username");
        String password = readSecretFile("password");
        File x509CertificateFile = new File(SECRETS_PATH, "certificate.pem");
        
        String x509Certificate = null;
        KeyStore trustStore = null;
        if (x509CertificateFile.exists()) {
            x509Certificate = readSecretFile("certificate.pem");
            trustStore = createTrustStore(x509Certificate);
        }
        
        return new AppCredentials(hostname, port, username, password, x509Certificate, trustStore);
    }
    
    private static KeyStore createTrustStore(String cert) throws Exception {
        KeyStore trustStore = KeyStore.getInstance("JKS");
        trustStore.load(null);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        trustStore.setCertificateEntry("messaging",
                cf.generateCertificate(new ByteArrayInputStream(cert.getBytes("UTF-8"))));
        return trustStore;
    }

    private static final String SECRETS_PATH = "/etc/app-credentials";

    private static final String readSecretFile(String filename) throws IOException {
        File secretDir = new File(SECRETS_PATH);
        File file = new File(secretDir, filename);
        if (!file.exists()) {
            throw new IllegalStateException("Unable to find secret " + file.getAbsolutePath());
        }
        return new String(Files.readAllBytes(file.toPath()));
    }

    private static boolean isOnKube() {
        return new File("/var/run/secrets/kubernetes.io/serviceaccount").exists();
    }

    public static AppCredentials fromProperties() throws Exception {
        Properties properties = loadProperties("config.properties");
        String cert = properties.getProperty("certificate.pem");
        KeyStore trustStore = null;
        if (cert != null) {
            trustStore = createTrustStore(cert);
        }
        return new AppCredentials(
                properties.getProperty("hostname"),
                Integer.parseInt(properties.getProperty("port")),
                properties.getProperty("username"),
                properties.getProperty("password"),
                cert,
                trustStore);
    }

    private static Properties loadProperties(String resource) throws IOException {
        Properties properties = new Properties();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream stream = loader.getResourceAsStream(resource);
        properties.load(stream);
        return properties;
    }
}
