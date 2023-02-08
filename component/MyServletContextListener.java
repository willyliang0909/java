package internal.server.servlet;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.SchedulerException;

import com.google.common.base.Throwables;
import com.mysql.cj.jdbc.AbandonedConnectionCleanupThread;

import internal.db.dao.frame.IDB_Info;
import internal.db.dao.frame.ITask;
import internal.db.util.DBInjector;
import internal.db.util.DBUtil;
import internal.schedule.ScheduleManager;
import internal.server.util.DataSourceManager;
import internal.server.util.DefaultValues;
import internal.server.util.GlobalVarManager;
import internal.server.util.Statistics;
import web.db.AliasDB;
import web.db.GlobalVarDB;

@WebListener
public class MyServletContextListener implements ServletContextListener {

	public static boolean isLicensed = false;
	private Logger logger = LogManager.getLogger(DefaultValues.serverLog);
	
		
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		
		logger.info("contextDestroyed");	
		closeDataSources();
		
		//
		try {
			LogManager.shutdown();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//
		try {
			ScheduleManager.destroy();
		} catch (SchedulerException e) {
			e.printStackTrace();
		}
		
		//
		try {
			DBUtil.destroy();
		} catch (SQLException e) {
			e.printStackTrace();
		}
				
		try {
			AbandonedConnectionCleanupThread.checkedShutdown();
		} catch (Exception e) {
			e.printStackTrace();
		}
	    
		
		Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            try {
                DriverManager.deregisterDriver(driver);
                logger.debug(String.format("deregistering jdbc driver: %s", driver));
            } catch (SQLException e) {
            	e.printStackTrace();
            }
        }	
	}

	
	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		
		//disable https host verify
		//final Properties props = System.getProperties(); 
		//props.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());
								
		//init Statistics start_time
		Statistics.getUpTime();
				
		if (dbConnected()) {
			initTask();
		} else { 
			new Thread(runnable).start();
		}		
	}
	
	void initTask() {
		
		initDataSources();
		initSchedule();		
		initGlobal();
		
		//disabled 20200712
		//在schedule, global之後
		//LicenseJob.checkLicense();
	}
	
	Runnable runnable = () -> {
		
		do {
			logger.fatal("Server database is not connected, initialize task failed.");			
			try {
				Thread.sleep(2*60*1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
				logger.error(Throwables.getStackTraceAsString(e));
			}
		} while (!dbConnected());
			
		initTask();
	};

	public void closeDataSources() {
		
		logger.info("Closing datasources...");
		
		IDB_Info db_info = DBInjector.getInstance(IDB_Info.class);		
		List<Map<String, String>> list = db_info.getEnableList();
		
		for (Map<String, String> map : list) {
			try {
				String alias = map.get(IDB_Info.Alias_Name);
				DataSourceManager.disable(alias);
			} catch (Exception e) {
				e.printStackTrace();
				logger.error(Throwables.getStackTraceAsString(e));
			}
		}		
		logger.info("Closing datasources complete.");
	}
	
	public void initDataSources() {
			
		logger.info("Initializing datasources...");
		
		IDB_Info db_info = DBInjector.getInstance(IDB_Info.class);
		
		List<Map<String, String>> list = db_info.getEnableList();
		
		for (Map<String, String> map : list) {
			String alias = map.get(IDB_Info.Alias_Name);
			String connection_status = "0";
			try {				
				DataSourceManager.enable(alias, map);	
				connection_status = "1";
			} catch (Exception e) {		
				e.printStackTrace();
				logger.error(Throwables.getStackTraceAsString(e));
				connection_status = "2";
				//disable alias 
				try {
					DataSourceManager.disable(alias);
				} catch (Exception e1) {
					e1.printStackTrace();
					logger.error(Throwables.getStackTraceAsString(e1));
				}				
			} finally {
				try {
					AliasDB aliasDB = new AliasDB();
					aliasDB.enabled_change(alias, "0", connection_status);
				} catch (Exception e) {
					e.printStackTrace();
					logger.error(Throwables.getStackTraceAsString(e));
				}				
			}
		}
		
		logger.info("Initializing datasources complete.");
		
	}
	
	public void initSchedule() {
		
		logger.info("Initializing schedule...");
		
		try {
			ScheduleManager.init();
			
			ITask task = DBInjector.getInstance(ITask.class);
			
			List<Map<String, String>> list = task.getEnableList();
			
			for (Map<String, String> map : list) {
				String id = map.get(ITask.TASK_ID);
				try {
					ScheduleManager.start(id);
				} catch (Exception e) {			
					e.printStackTrace();
					logger.error(Throwables.getStackTraceAsString(e));
				}			
			}
			
			logger.info("Initializing schedule complete.");
			
		} catch (SchedulerException e1) {
			e1.printStackTrace();
			logger.error("Initializing schedule failed.");
			logger.error(Throwables.getStackTraceAsString(e1));
		}				
	}
	 
	public void initGlobal() {
		
		try {
			GlobalVarDB global = new GlobalVarDB();
			Map<String, String> var_map = new HashMap<>();
			
			var data_list = global.get_all();
			
			for (Map<String, String> map : data_list) {
				var_map.put(map.get("key"), map.get("value"));
			}
			
			GlobalVarManager.reload(var_map);
		} catch (Exception e) {			
			e.printStackTrace();
			logger.error(Throwables.getStackTraceAsString(e));
		}	
		
	}
	
	public boolean dbConnected() {
		
		Connection conn = null;
		try {
			conn = DBUtil.getConnection();
			return true;
		} catch (SQLException e) {
			logger.error(Throwables.getStackTraceAsString(e));
			return false;
		} finally {
			DBUtil.closeConnection(conn);
		}
	}
}
