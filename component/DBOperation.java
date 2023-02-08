package web.util;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import internal.server.util.AES;

@SuppressWarnings("deprecation")
public class DBOperation {

	
	@SuppressWarnings("unchecked")
	public static JSONArray extractSqlResult(ResultSet rs, String... decrypt_fileds) throws Exception {
		
		JSONArray jsonArray = new JSONArray();
		
		List<Map<String, String>> data_list = extractResult(rs);
		
		
		for (Map<String, String> map : data_list) {
			JSONObject jsonObject = new JSONObject();
			for (String field : decrypt_fileds) {
				String pass = AES.Decrypt(map.get(field));
				map.put(field, pass);
			}
			jsonObject.putAll(map);
			jsonArray.add(jsonObject);
		}
		/*
		List<String> columnNames = new ArrayList<>();
		
		ResultSetMetaData rsmd = rs.getMetaData();
		Map<String, String> map = new HashMap<>();
		
		for (int i = 1; i < rsmd.getColumnCount() +1; i++) {
			columnNames.add(rsmd.getColumnLabel(i));
			//System.out.println(rsmd.getColumnLabel(i));
		}
		while (rs.next()) {		
			for (String columnName : columnNames) {
				map.put(columnName, rs.getString(columnName));	
			}
			JSONObject jsonObject = new JSONObject();
			jsonObject.putAll(map);
			jsonArray.add(jsonObject);
		}
		*/
		return jsonArray;
	}
	
	public static List<Map<String, String>> extractResult(ResultSet rs) throws Exception {
		
		List<Map<String, String>> data_list = new ArrayList<>();
		List<String> columnNames = new ArrayList<>();
		
		ResultSetMetaData rsmd = rs.getMetaData();
				
		for (int i = 1; i < rsmd.getColumnCount() +1; i++) {
			columnNames.add(rsmd.getColumnLabel(i));
			//System.out.println(rsmd.getColumnLabel(i));
		}
		while (rs.next()) {		
			Map<String, String> map = new HashMap<>();
			for (String columnName : columnNames) {
				map.put(columnName, rs.getString(columnName));	
			}
			data_list.add(map);
		}
		return data_list;
		
	}
}
