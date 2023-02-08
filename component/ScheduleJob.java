package internal.schedule;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;

import com.google.common.base.Throwables;

import internal.db.dao.frame.ITask;
import internal.db.util.DBInjector;
import internal.server.util.DefaultValues;
import internal.server.util.Statistics;
import service.IService;

public class ScheduleJob implements Job {

	public static final String STATE_RUNNING = "2";
	public static final String STATE_START = "1";
	public static final String STATE_STOP = "0";
	
	private Logger logger = LogManager.getLogger(DefaultValues.serverLog);  
	private ITask task = DBInjector.getInstance(ITask.class);
	SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		
		long start = System.currentTimeMillis();
		String id = "";
		String className = "";
		try {
			
			JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
			id = jobDataMap.getString(ITask.TASK_ID);
			
			String task_name = jobDataMap.getString(ITask.TASK_NAME);
			className = jobDataMap.getString(ITask.SERVICE);
						
			logger.info("Schedule: " +  task_name + " is running.");
			
			
			//run service
			Class<?> clazz = Class.forName(className);
			IService service = (IService) clazz.getDeclaredConstructor().newInstance();
			service.execute(null, null);							
			
		} catch (Exception e) {	
			e.printStackTrace();
			logger.error(Throwables.getStackTraceAsString(e));
			//update schedule last error
			Statistics.addErrorCount(className);
			update_last_error(id, e);
		} finally {
			//update next run
			Date nextRun = context.getNextFireTime();
			if (nextRun != null) {
				String nextRun_s = format.format(nextRun);
				update_next_run(id, nextRun_s);
			} else {
				try {
					ScheduleManager.stop(id);
				} catch (SchedulerException e) {
					e.printStackTrace();
					logger.error(Throwables.getStackTraceAsString(e));
				}
			}
			
			long stop = System.currentTimeMillis();
			Statistics.addServiceRunTime(className, (stop - start));
		}
		
		
	}
	
	//update IS_USER_TASKS.NEXTRUN
	void update_next_run(String id, String nextRun) {
		
		try {				
			//String nextRun_s = format.format(context.getNextFireTime());
			logger.debug("NEXTRUN: " + nextRun);
			
			Map<String, String> updateMap = new HashMap<>();
			//updateMap.put(ITask.STATE, Schedule.STATE_START);			
			updateMap.put(ITask.NEXTRUN, nextRun);
			updateMap.put(ITask.LASTRUN, format.format(new Date()));
			task.update(id, updateMap);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(Throwables.getStackTraceAsString(e));
		}		
	}
	
	//update IS_USER_TASKS.LASTERROR
	void update_last_error(String id, Exception e) {
		
		//update schedule last error
		try {
			String now = format.format(new Date());
			String exception_class = e.getClass().getCanonicalName();
			
			Map<String, String> updateMap = new HashMap<>();
			updateMap.put(ITask.LASTERROR, now + " - " + exception_class + "<br>" + e.getMessage());
			task.update(id, updateMap);
		} catch (Exception e1) {
			e1.printStackTrace();
			logger.error(Throwables.getStackTraceAsString(e1));					
		}
		
	}

}
