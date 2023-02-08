package pub.client;


import java.io.File;
import java.util.List;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;


public class SendMail {
	
	
	public static void send(String host, List<String> mailTo, String mailFrom, String subject, String msg, File file, String userName, String password) {
		
		new Thread(() -> {
			HtmlEmail email = new HtmlEmail();
	    		
	    		try {
	    			email.setHostName(host);
	    			//email.setCharset("BIG5");
	    			email.setCharset("utf-8");
	    			// 收件人
	    			for (String to : mailTo) {
	    				email.addTo(to);		
	    			}
	    					
	    			// 邮箱服务器身份验证
	    			//email.setAuthentication("你的邮箱地址", "你的邮箱密码");
	    			
	    			email.setFrom(mailFrom);
	    			email.setSubject(subject);
	    			email.setMsg(msg);
	    			//email.setTextMsg(msg);
	
	    			if (file != null) {
	    				email.attach(file);
	    			}	
	    			
	    			if (userName != null) {
	    				email.setAuthentication(userName, password);
	    			} 
	    			email.setAuthenticator(null);
	    			
	    			
	    			email.send();
	    		} catch (EmailException ex) {
	    			ex.printStackTrace();
	    		}
		}).start();
		
	}
	
	/*
	public static void send() {
		SimpleEmail email = new SimpleEmail();
		email.setHostName("172.16.1.160");// 设置使用发电子邮件的邮件服务器，这里以qq邮箱为例（其它例如：【smtp.163.com】，【smtp.sohu.com】）
		try {
			// 收件人邮箱
			email.addTo("willy_liang@ecomsoft.com.tw");				
			// 邮箱服务器身份验证
			//email.setAuthentication("你的邮箱地址", "你的邮箱密码");
			// 发件人邮箱
			email.setFrom("test_test@ecomsoft.com.tw");
			// 邮件主题
			email.setSubject("test-JavaMail");
			// 邮件内容
			email.setMsg("Kobe Bryante Never Stop Trying");
			// 发送邮件
			email.send();
		} catch (EmailException ex) {
			ex.printStackTrace();
		}
	}*/
		
	public static void sendHtml() {
		HtmlEmail email = new HtmlEmail();
		email.setHostName("172.16.1.160");// 设置使用发电子邮件的邮件服务器，这里以qq邮箱为例（其它例如：【smtp.163.com】，【smtp.sohu.com】）
		try {
			// 收件人邮箱
			email.addTo("willy_liang@ecomsoft.com.tw");
			email.setCharset("BIG5");
			// 邮箱服务器身份验证
			//email.setAuthentication("你的邮箱地址", "你的邮箱密码");
			// 发件人邮箱
			email.setFrom("test_test@ecomsoft.com.tw");
			// 邮件主题
			email.setSubject("test-JavaMail");
			// 邮件内容
			//email.setMsg("Kobe Bryante Never Stop Trying");
			email.setMsg("<h1 style='color:red'>下午3：00會議室</h1>" + " 請準時參加！");
			// 发送邮件
			 
			/*
			File pdf = new File("/Users/ecomnb/Desktop/SendMail.java");
			try {
				email.embed(pdf.toURI().toURL(), pdf.getName());
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			*/
			
			File file = new File("/Users/ecomnb/Desktop/SendMail.java");
			email.attach(file);
			
			email.send();
		} catch (EmailException ex) {
			ex.printStackTrace();
		}

	}
	
	public static void sendAttach() {
		//File file = new File("/Users/ecomnb/Desktop/pic07.png");
		//email.attach(file);
	}
}
