package com.utils;

import java.io.IOException;
import java.sql.*;
import java.util.Properties;


public class Dbconnection {
	
	private Dbconnection() {}
	
	static Properties properties = null;
	
	public static Connection getConn() throws SQLException, IOException {
					
		Connection conn = null;
		
		if (properties == null) {
			properties = ConfigUtil.loadConfig(); 
		}
		
		String jdbcUrl = properties.getProperty("db.local.jdbcUrl");
		String username = properties.getProperty("db.local.username");
		String password = properties.getProperty("db.local.password");
					
		conn = DriverManager.getConnection(jdbcUrl, username, password);			
		
		return conn;		
	}
	
	public static void close(Connection conn) {
		if (conn != null) {
			try {
				conn.close();
			} catch(SQLException e) {
				e.printStackTrace();
			}
		}
	}
}
