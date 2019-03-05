package subbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@SpringBootApplication
public class SubBoxApplication {

    private static final Logger log = LoggerFactory.getLogger(SubBoxApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(SubBoxApplication.class, args);
    }

    public static String getProperty(String key) {
        return Lazy.properties.getProperty(key);
    }

    private static class Lazy {
        static final Properties properties = loadProperties();

        private static Properties loadProperties() {
            Properties properties = new Properties();

            try (InputStream inStream = SubBoxApplication.class.getClassLoader().getResourceAsStream("application.properties")) {
                properties.load(inStream);
            } catch (IOException e) {
                log.error("Loading properties failed", e);
            }

            return properties;
        }
    }

}
