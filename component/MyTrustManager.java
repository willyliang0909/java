

import java.security.cert.CertificateException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.cert.X509Certificate;

@SuppressWarnings("deprecation")
public class MyTrustManager implements X509TrustManager {

	public java.security.cert.X509Certificate[] getAcceptedIssuers() {
	   return null;
	}

	public void checkClientTrusted(X509Certificate[] certs, String authType) {
	}

	public void checkServerTrusted(X509Certificate[] certs, String authType) {
	}

	@Override
	 public void checkClientTrusted(java.security.cert.X509Certificate[]      paramArrayOfX509Certificate, String paramString)
	    throws CertificateException {
	  // TODO Auto-generated method stub

	}

	@Override
	public void checkServerTrusted(java.security.cert.X509Certificate[] paramArrayOfX509Certificate, String paramString)
	        throws CertificateException {
	    // TODO Auto-generated method stub

	}
		
	public static void disableSSL() {
	
		try {
			TrustManager[] trustAllCerts = new TrustManager[] { new   MyTrustManager() };

			// Install the all-trusting trust manager
		    SSLContext sc = SSLContext.getInstance("SSL");
		    sc.init(null, trustAllCerts, new java.security.SecureRandom());
		    HostnameVerifier allHostsValid = new HostnameVerifier() {
	        public boolean verify(String hostname, SSLSession session) {
	            return true;
	        }
	    };
	    HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
	    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
	} catch (Exception e) {
	    e.printStackTrace();
	}
	      
	}
}
