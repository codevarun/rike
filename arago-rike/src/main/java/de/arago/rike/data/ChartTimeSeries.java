/**
 * Copyright (c) 2010 arago AG, http://www.arago.de/
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package de.arago.rike.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minidev.json.JSONArray;

import org.hibernate.Hibernate;


public class ChartTimeSeries {
	
	//private static final SessionFactory factory;
	
	public static String releaseTasksStatus = 
					"SELECT sum(summe_size) as value, task_status as name, moment "
					+ "FROM task_stat,milestones "
					+ "WHERE milestone_id=milestones.id AND milestones.release_name=? "
					+ "GROUP BY task_status, moment";
	public static String milestoneTasksStatus =
					"SELECT sum(summe_size) as value, task_status as name, moment "
					+ "FROM task_stat "
					+ "WHERE milestone_id = ? "
					+ "GROUP BY task_status, moment";
	public static String allTasksStatus = 
					"SELECT sum(summe_size) as value, task_status as name, moment "
					+ "FROM task_stat "
					+ "GROUP BY task_status, moment";
	public static String milestoneBurndown = 
					"SELECT sum( summe_size ) as value, milestones.title as name, moment "
					+ "FROM task_stat, milestones "
					+ "WHERE milestone_id = milestones.id "
					+ "AND task_status != 'done' "
					+ "AND due_date IS NOT NULL "
					+ "GROUP BY milestones.title, moment "
					+ "ORDER BY due_date, moment";
	public static String allBurndown = 
		"SELECT sum( summe_size ) as value, milestones.release_name as name, moment "
		+ "FROM task_stat, milestones "
		+ "WHERE milestone_id = milestones.id "
		+ "AND task_status != 'done' "
		+ "AND due_date IS NOT NULL "
		+ "GROUP BY milestones.release_name, moment "
		+ "ORDER BY moment";
	public static String releaseBurndown = 
		"SELECT sum( summe_size ) as value, milestones.title as name, moment "
		+ "FROM task_stat, milestones "
		+ "WHERE milestone_id = milestones.id "
		+ "AND task_status != 'done' "
		+ "AND milestones.release_name=? "
		+ "AND due_date IS NOT NULL "
		+ "GROUP BY milestones.title, moment "
		+ "ORDER BY due_date, moment";
	
	
	public static Map<String,List<List<Long>>> query(String str, Object[] parameters){
		//Session s = factory.getCurrentSession();
		//Transaction tr = s.beginTransaction();
		DataHelperRike<Object> helper = new DataHelperRike<Object>(Object.class);
		org.hibernate.SQLQuery query = helper.createSQLQuery(str)
											.addScalar("name", Hibernate.STRING)
											.addScalar("value",Hibernate.LONG)
											.addScalar("moment", Hibernate.DATE);

		if (parameters != null)
		{
			for (int i = 0; i < parameters.length; ++i)
			{
				query.setParameter(i, parameters[i]);
			}
				
		}
		
		Map<String,List<List<Long>>> data = new LinkedHashMap<String,List<List<Long>>>();
		for(Object first:query.list()){
			Object[] arr = (Object[])first;
			String name = (String)arr[0];
			Long value = (Long)arr[1];
			Date moment = (Date)arr[2];
			List<List<Long>> ts;
			if(!data.containsKey(name)){
				ts = new LinkedList<List<Long>>();
				data.put(name, ts);
			}else
				ts = data.get(name);
			List<Long> tmp = new ArrayList<Long>(2);
			tmp.add(moment.getTime());
			tmp.add(value);
			ts.add(tmp);
		}
    
		helper.finish(query);
		//tr.commit();
		return data;
	}

	final static String[] names = {"done","in_progress","open","unknown"};
	final static String[] labels = {"Erledigt","in Bearbeitung","Offen","noch nicht bewertet"};
	final static String[] colors = {"green","yellow","red","blue"};

	public static List<Map<String,Object>> taskStatusJSON(String query, Object[] parameters){
		Map<String,List<List<Long>>> data = query(query, parameters);
		ArrayList<Map<String,Object>> list = new ArrayList<Map<String,Object>>(data.size());
		for( int i=0; i<names.length; i++ ){
			if(data.containsKey(names[i])){
				TreeMap<String,Object> map = new TreeMap<String, Object>();
				map.put("label",labels[i]);
        map.put("key", names[i]);
				map.put("color",colors[i]);
				map.put("data",data.get(names[i]));
				list.add(map);
			}
		}
		return list;
	}
	
	
	private static void stackData(List<Map<String,Object>> list){
		TreeMap<Long,Long> dates = new TreeMap<Long,Long>();
		for(Map<String,Object> m:list){
			List<List<Long>> tmp = (List<List<Long>>)m.get("data");
			for(List<Long> l:tmp){
				Long sum;
				if(dates.containsKey(l.get(0)))
					sum = dates.get(l.get(0));
				else
					sum = new Long(0);
				sum = new Long(sum.longValue()+l.get(1).longValue());
				l.set(1, sum);
				dates.put(l.get(0),sum);
			}
		}
	}

	
	private static void clearData(List<Map<String,Object>> list){
		TreeMap<Long,Long> dates = new TreeMap<Long,Long>();
		for(Map<String,Object> m:list){
			List<List<Long>> tmp = (List<List<Long>>)m.get("data");
			for(List<Long> l:tmp){
				Long sum;
				if(dates.containsKey(l.get(0)))
					sum = new Long(dates.get(l.get(0)).longValue()+1);
				else
					sum = new Long(0);
				dates.put(l.get(0),sum);
			}
		}
		List<List<Long>> toRemove = new ArrayList<List<Long>>(dates.size());
		for(Map<String,Object> m:list){
			List<List<Long>> tmp = (List<List<Long>>)m.get("data");
			toRemove.clear();
			for(List<Long> l:tmp){
				Long sum = dates.get(l.get(0));
				if(sum.longValue()==0)
					toRemove.add(l);
			}
			tmp.removeAll(toRemove);
		}
	}

	public static List<Map<String,Object>> toBurndownJSON(String query, Object[] parameters){
		Map<String,List<List<Long>>> data = query(query,parameters);
		ArrayList<Map<String,Object>> list = new ArrayList<Map<String,Object>>(data.size());
		for(String name:data.keySet()){
			TreeMap<String,Object> map = new TreeMap<String, Object>();
			map.put("label",name);
      map.put("key", name);
			map.put("data",data.get(name));
			list.add(map);
		}
		return list;
	}
	
	public static String toPrettyJSON(String type, String milestone) {
		List<Map<String,Object>> list = null;
		if (type.equals("burndown")) {
			if (milestone.startsWith("release_"))
				list = toBurndownJSON(releaseBurndown, new Object[]{milestone.substring(8)});
			else if(milestone.startsWith("milestone_"))
				list = toBurndownJSON(milestoneBurndown, null);
			else
				list = toBurndownJSON(allBurndown, null);
		} else if (type.equals("taskstatus")) {
			if (milestone == null || milestone.isEmpty()) {
				list = taskStatusJSON(allTasksStatus, null);
			} else {
				if (milestone.startsWith("milestone_")) {
					list = taskStatusJSON(milestoneTasksStatus, new Object[]{milestone.substring(10)});
				} else if (milestone.startsWith("release_")) {
					list = taskStatusJSON(releaseTasksStatus, new Object[]{milestone.substring(8)});
				}
			}
		}
		if(list==null)
			return "";
		if (type.equals("taskstatus"))
			clearData(list);
		stackData(list);
		return JSONArray.toJSONString(list);
	}
	
}
