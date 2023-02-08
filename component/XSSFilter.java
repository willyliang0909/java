package internal.server.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class XSSFilter implements Filter {

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {		
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;

        // 建立HttpServletRequestWrapper，包裝原HttpServletRequest物件，示例程式只重寫了getParameter方法，
        // 應當考慮如何過濾：getParameter、getParameterValues、getParameterMap、getInputStream、getReader
        HttpServletRequestWrapper requestWrapper = new HttpServletRequestWrapper(request) {
            public String getParameter(String name) {
                // 獲取引數值
                String value = super.getParameter(name);

                if (value == null) {
                	return null;
                } else {
                	// 簡單轉義引數值中的特殊字元
                    return value.replace(">","&gt;").replace("<", "&lt;")
                    		.replace("&", "&amp;").replace("'", "&#039;");
                }
                
            }
        };

        chain.doFilter(requestWrapper, resp);
	}

	@Override
	public void destroy() {		
	}

}
