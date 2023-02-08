package com.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Throwables;

public class AdUtil {

	private AdUtil() {}
	
	
	static final Logger logger = LogManager.getLogger();
	
	public enum adColumn { sAMAccountName, displayName, mail, memberOf, distinguishedName, info}
	
	public static List<Map<String, String>> query(InitialLdapContext context, String baseDN, String filter, String... attrIDs) throws IOException, NamingException {
		
		logger.debug("ad query with baseDN: {}, filter: {}", baseDN, filter);
		
		List<Map<String, String>> list = new ArrayList<>();		
			
		SearchControls cons = new SearchControls();
        cons.setSearchScope(SearchControls.SUBTREE_SCOPE);
        cons.setReturningObjFlag(true);

        if (attrIDs.length > 1) {
        	cons.setReturningAttributes(attrIDs);
        }	        
        
        NamingEnumeration<SearchResult> sEnum = context.search(baseDN, filter, cons);
       
        while (sEnum.hasMoreElements()) {           
        	SearchResult rs = sEnum.nextElement();     		
        	Map<String, String> map = new HashMap<>();            	
        	NamingEnumeration<? extends Attribute> aEnum = rs.getAttributes().getAll();
        	while (aEnum.hasMoreElements()){
                Attribute attr = aEnum.nextElement();
                String id = attr.getID();
                String value = adValueProcess(attr);
                map.put(id, value);
        	}
        	list.add(map);
        }        
        return list;
	}

	private static String adValueProcess(Attribute attr) throws NamingException {
		
		String id = attr.getID();
		
		List<String> values = new ArrayList<>();
	    for (int i=0; i<attr.size(); i++ ) {
	    	String val = (String) attr.get(i);
	    	switch (adColumn.valueOf(id)) {
			case displayName:
				//get name of #部門-姓名
				String[] names = val.split("-");
	    		if (names.length > 1) {
	    			val = names[1];
	    		}
				break;
			case memberOf:
				//get CN=...
				val = val.substring(3, val.indexOf(","));   
				break;			
			case distinguishedName:
			case mail:
			case info:				
			default:
				break;
	    	}
	    	
	    	values.add(val);
	    }
	    
	    return  String.join(",", values);
	}

	public static InitialLdapContext getContext(String url, String user, String password) throws IOException, NamingException {
			
		Hashtable<String, String> HashEnv = new Hashtable<>();
		HashEnv.put("com.sun.jndi.ldap.read.timeout", "30000");
    	HashEnv.put("com.sun.jndi.ldap.connect.timeout", "10000");
    	
    	HashEnv.put(Context.PROVIDER_URL, url);
    	HashEnv.put(Context.SECURITY_PRINCIPAL, user); // AD User
    	HashEnv.put(Context.SECURITY_CREDENTIALS, password); // AD Password        	
    	HashEnv.put(Context.SECURITY_AUTHENTICATION, "simple"); // LDAP訪問安全級別        	
    	HashEnv.put(Context.INITIAL_CONTEXT_FACTORY,"com.sun.jndi.ldap.LdapCtxFactory"); // LDAP工廠類
    	
    	return new InitialLdapContext(HashEnv, null);
		
	}
		
	public static void closeContext(InitialLdapContext context) {
		if (context != null) {
			try {
				context.close();
			} catch (NamingException e) {
				logger.error(Throwables.getStackTraceAsString(e));
			}
		}
	}
	
		
}
