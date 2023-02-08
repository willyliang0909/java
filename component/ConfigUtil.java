package com.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConfigUtil {

	static final Logger logger = LogManager.getLogger();
	static Properties prop = null;
	
	private ConfigUtil() {
		
	}
	
	public static Properties loadConfig() throws IOException {
		
		return loadConfig("apm.conf");
	}
	
	public static Properties loadConfig(String name) throws IOException {
		
		if (prop != null) {
			return prop;
		} else {
			Properties prop = new Properties();
			String home = System.getenv("APM_REPORT_HOME");
			String fileLocation = home + File.separator + "conf" +  File.separator + name;
			
			logger.debug("reading config from {}", fileLocation);
			
			try ( final var in = new InputStreamReader(
				      new FileInputStream( new File(fileLocation) ), StandardCharsets.UTF_8 ) ) {
				prop.load( in );
			}
			
			logger.debug("properties: {}", prop);
			
			return prop;
			
		}
		
		
	}
	
}
