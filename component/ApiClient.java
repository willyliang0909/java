package com.report.acl;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class ApiClient {

	public ApiClient() {
		System.getProperties().setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());		    
	}
	
	public String call(String url, String account, String password) throws Exception {
			    
	    String auth = account+ ":" + password;	   	
	   	String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
	   	
	   	HttpClient client = HttpClient.newBuilder()
	   			.sslContext(getSSLContext())
	   			.build();
	   	
	   	HttpRequest request = HttpRequest.newBuilder()
	   			.uri(URI.create(url))
	   			.header("Authorization", "Basic " + encodedAuth)
	   			.GET()
	   			.build();
	   	
	   	HttpResponse<String> rs = null;
	   	
	   	rs = client.send(request, HttpResponse.BodyHandlers.ofString());
	   	
	   	if (rs.statusCode() != 200) {
	   		throw new Exception("status: " + rs.statusCode() + ", body: " + rs.body());
	   	}
	   	
	   	return rs.body();
	}
	
	
	private SSLContext getSSLContext() throws Exception {
		
		SSLContext sslContext;	
		try {
			sslContext = SSLContext.getInstance("TLS");
		    sslContext.init(null, trustAllCerts(), new java.security.SecureRandom());
	    } catch (Exception e) {
	    	throw new Exception("TLS init failed.", e);
	    }
		
		return sslContext;
	}
	
	private TrustManager[] trustAllCerts() {
		return new TrustManager[]{
		    new X509TrustManager() {
				@Override
				public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
				@Override
				public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
				@Override
				public X509Certificate[] getAcceptedIssuers() { return null; }
		    	
		    }
		};
	}
	
}
