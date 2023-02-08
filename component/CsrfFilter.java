package internal.server.filter;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class CsrfFilter implements Filter  {
	
	private final Set<String> excludePages = new HashSet<>();
	final String token_name = "csrf_token";

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		String excludePage = filterConfig.getInitParameter("excludePage");
		if (excludePage != null) {
			String values[] = excludePage.split(",");
			for (String value : values) {
				this.excludePages.add(value.trim());
			}
		}
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		
		HttpServletRequest req = (HttpServletRequest)request; 
		HttpSession s = req.getSession();
		
		if (excludePages.contains(req.getServletPath())) {
			chain.doFilter(request, response); 
		} else {
			// 從 session 中得到 csrftoken 屬性
			String sToken = (String) s.getAttribute(token_name); 
			// 從請求引數中取得 csrftoken 
		    String pToken = req.getParameter(token_name); 
		        	    
		    //比對session以及request token是否相同
		    if (sToken != null && pToken != null && sToken.equals(pToken)) {	    	
		    	chain.doFilter(request, response); 
		    } else {
		    	HttpServletResponse rs = (HttpServletResponse) response;
		    	rs.sendError(HttpServletResponse.SC_FORBIDDEN);
		    }
		}				
	}

	@Override
	public void destroy() {		
	}

}
