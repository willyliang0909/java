package internal.schedule;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

import com.google.common.base.Throwables;

import internal.db.dao.frame.ITask;
import internal.db.util.DBInjector;
import internal.server.servlet.MyServletContextListener;
import internal.server.util.DefaultValues;

public class ScheduleManager {

	private static ITask task = null;
	private static Logger logger = LogManager.getLogger(DefaultValues.serverLog);
	private static Map<String, JobKey> jobMap = new HashMap<>();
	
	
	private static SchedulerFactory sf = new StdSchedulerFactory();
	private static Scheduler scheduler = null;
	
	static JobDetail shutdown_job = JobBuilder.newJob(ShutDownJob.class)
		    .withIdentity("shutdown job","server")
		    .build();
	
	static {
		task = DBInjector.getInstance(ITask.class);		
	}
	
	private ScheduleManager() {
		super();
	}
	
	public static void init() throws SchedulerException {
		if (scheduler == null || scheduler.isShutdown()) {
			scheduler = sf.getScheduler();
			scheduler.start();
			
			//disabeld 20200721
			//addLicenseJob();
		}		
	}
	
	public static void destroy() throws SchedulerException {	
		if (scheduler != null) scheduler.shutdown(true);			
	}		
	
	
	public static void main(String[] args) throws Exception  {
		MyServletContextListener m = new MyServletContextListener();
		m.contextInitialized(null);
	}
	/*
	private static void update(String id, String state, String nextRun) throws Exception {		
		Map<String, String> updateMap = new HashMap<>();
		updateMap.put(ITask.STATE, state);
		updateMap.put(ITask.NEXTRUN, nextRun);
		task.update(id, updateMap);		
	}
	*/
	/*
	private static void addLicenseJob() throws SchedulerException {
		
		JobDetail license_job = JobBuilder.newJob(LicenseJob.class)
			    .withIdentity("license checker","server")
			    .build();
		
		Trigger trigger = TriggerBuilder.newTrigger()
			.withSchedule(CronScheduleBuilder.cronSchedule("0 0 12 * * ?"))
			.build();
		
		scheduler.scheduleJob(license_job, trigger);
	}
	*/
	public synchronized static void addShutDownJob() throws SchedulerException {
		
		if (scheduler.checkExists(shutdown_job.getKey())) {
			logger.warn("Schedule shutdown_job already exist.");
		} else {
			Trigger trigger = TriggerBuilder.newTrigger()
					.startAt(new Date(System.currentTimeMillis() + 60*60*1000))
					.build();
			
			scheduler.scheduleJob(shutdown_job, trigger);
		}
		
	}
	
	public synchronized static void stopShutDownJob() throws SchedulerException {
		scheduler.deleteJob(shutdown_job.getKey());
	}
	
	public static String getShutDownTime() throws SchedulerException {
		
		String time = "";
		if (scheduler != null) {
			var list = scheduler.getTriggersOfJob(shutdown_job.getKey());
			
			if (!list.isEmpty()) {
				Date next = list.get(0).getNextFireTime();
				time = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(next);
			} 			
		}
		
		return time;
	}
	
	//stop schedule
	public synchronized static void stop(String id) throws SchedulerException {
		
		JobKey jobKey = jobMap.get(id);
		
		if (jobKey == null) {
			logger.warn("Stop schedule failed, schedule id: " + id + " not found or is not running.");
			jobMap.remove(id);
			update_db(id, ScheduleJob.STATE_STOP, null);
		} else {
			String task_name = jobKey.getName();
			try {				
				logger.info("Stopping schedule: " + task_name + "...");
				
				scheduler.deleteJob(jobKey);
				jobMap.remove(id);
				
				update_db(id, ScheduleJob.STATE_STOP, null);
				
				logger.info("Schedule: " + task_name + " is stopped");
			} catch (SchedulerException e) {
				logger.error("Stop schedule" + task_name + " failed." );
				throw e;
			}
		}		
	}
	
	
	
	//start schedule
	public synchronized static void start(String id) throws ParseException, SchedulerException, InterruptedException {
		
		if (scheduler == null) {
			init();
		}
		
		//get schedule config from db
		Map<String, String> dataMap = task.selectOne(id);
		if (dataMap == null || dataMap.isEmpty()) {
			logger.error("No schedule found by id = " + id);
		} else {
			
			//get setting from db 
			String task_name = dataMap.get(ITask.TASK_NAME);
			String mode =  dataMap.get(ITask.MODE);
			String startTime_s = dataMap.get(ITask.STARTTIME);
			String endTime_s = dataMap.get(ITask.ENDTIME);
			
			logger.info("Starting schedule: " + task_name + "(" + id + ")...");
			
							
			//set job
			JobDetail job = JobBuilder.newJob(ScheduleJob.class)
				    .withIdentity(task_name + "(" + mode + ")", id)
				    .usingJobData(ITask.TASK_ID, id)
				    .usingJobData(ITask.TASK_NAME, task_name)
				    .usingJobData(ITask.SERVICE, dataMap.get(ITask.SERVICE))
				    .build();
			
						
			//--- set trigger ---
			var triggerBuilder = TriggerBuilder.newTrigger()
				    .withIdentity("trigger - " + task_name + "(" + mode + ")", id);
			
			
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");	
			Date now = new Date();
			Date startTime = now;
			
			//start time
			if (startTime_s != null && !startTime_s.isEmpty()) {
				Date startTime_temp = sdf.parse(startTime_s);
				if (startTime_temp.after(now)) {
					startTime = startTime_temp;
				}
			}
			
			
			//end time
			if (endTime_s != null && !endTime_s.isEmpty()) {
				Date endTime = sdf.parse(endTime_s);
				triggerBuilder.endAt(endTime);
			}
			
			//
			if (mode.equals(ITask.MODE_INTERVAL)) {
				int interval = Integer.parseInt(dataMap.get(ITask.TIMEINTERVAL));
				String interval_type = dataMap.get(ITask.INTERVAL_TYPE);
				
				Calendar calendar = Calendar.getInstance(); 
				calendar.setTime(startTime); 
				
				switch (interval_type) {
				case "Second":
					triggerBuilder.withSchedule(SimpleScheduleBuilder.repeatSecondlyForever(interval));
					calendar.add(Calendar.SECOND, interval);
					break;
				case "Minute":
					triggerBuilder.withSchedule(SimpleScheduleBuilder.repeatMinutelyForever(interval));
					calendar.add(Calendar.MINUTE, interval);
					break;
				case "Hour":
					triggerBuilder.withSchedule(SimpleScheduleBuilder.repeatHourlyForever(interval));
					calendar.add(Calendar.HOUR, interval);
					break;
				default:
					throw new SchedulerException("Unsupport interval type: " + interval_type);
				}
				
				startTime = calendar.getTime();
				
				
			} else if (mode.equals(ITask.MODE_COMPLEX)) {
				
				String month_mask = dataMap.get(ITask.MONTHMASK);
				String day_mask = dataMap.get(ITask.DAYMASK);
				String week_mask = dataMap.get(ITask.WEEKMASK);
				String hour_mask = dataMap.get(ITask.HOURMASK);
				String minute_mask = dataMap.get(ITask.MINUTEMASK);
				
				String mask = "0 " +  minute_mask + " " + hour_mask + " " + day_mask + " " + month_mask + " " + week_mask; 
				
				
				
				triggerBuilder.withSchedule(CronScheduleBuilder.cronSchedule(mask));
				
			}
			triggerBuilder.startAt(startTime);
			//--- set trigger ---	
			
			
			Trigger trigger = triggerBuilder.build();
			
			
			//start schedule
			try {
				scheduler.scheduleJob(job, trigger);
				jobMap.put(id, job.getKey());
				
				String next_run = sdf.format(trigger.getNextFireTime());
				update_db(id, ScheduleJob.STATE_START, next_run);
				
				logger.info("Schedule: " + task_name + "(" + id + ") is started");
			} catch (SchedulerException e) {
				logger.error("Starting schedule" + task_name + "(" + id + ") failed." );
				throw e;
			}
			
			
		}
	}
	
	//update status, next_run
	private static void update_db(String id, String status, String next_run) {
		try {			
			Map<String, String> updateMap = new HashMap<>();
			updateMap.put(ITask.STATE, status);
			updateMap.put(ITask.NEXTRUN, next_run);
			task.update(id, updateMap);		
		} catch (Exception e) {
			logger.error("Schedule: " + id + " update failed.");
			logger.error(Throwables.getStackTraceAsString(e));
		}
	}
}
