package application;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import application.Model.ModifyResult;

import java.sql.*;

public class Controller {
	//model instance, controller communicate just with the model
	//Don't use javaFX imports classes, etc.
	private Model model;

	public Controller(){
		model = new Model();
	}


	/**
	 * Connect to DB with model
	 * @param userName Your DB username
	 * @param password Your DB password
	 * @param log Log container
	 * @return true if connect success else false
	 */
	public boolean connect(String userName, String password, List<String> log){
		if (model.connect(userName, password)) {
			// Test the connection
			String results = model.testConnection();
			if (results != null) {
				log.add("Connection seems to be working.");
				log.add("Connected to: '" + model.getDatabaseUrl() + "'");
				log.add(String.format("DBMS: %s, version: %s", model.getDatabaseProductName(),
						model.getDatabaseProductVersion()));
				log.add(results);
				return true;
			}
		}
		//always log
		log.add(model.getLastError());
		return false;
	}
	
	public List<String[]> listAll(List<String> log){
		List<String[]> result = new ArrayList<>();
		ResultSet rset = model.listAll();
		
		try {
			while(rset.next()) {
				
				String[] string = {rset.getString(1), rset.getString(2), 
						rset.getString(3)};
				result.add(string);
					
			}
			rset.close();
			return result;
		} catch (SQLException e) {
			model.lastError = "error ".concat(e.toString());
			return null;
		}
	}

	/**
	 * Task 1: Search with keyword
	 * USE: model.search
	 * Don't forget close the statement!
	 * @param keyword the search keyword
	 * @param log Log container
	 * @return every row in a String[],and the whole table in List<String[]>
	 */
	public List<String[]> search(String keyword, List<String> log){
		List<String[]> result = new ArrayList<>();
		
		ResultSet rset = model.search(keyword);
		
		try {
			while(rset.next()){
				String[] string = {rset.getString(1), rset.getString(2), 
						rset.getString(3)};
				result.add(string);
			}
			rset.getStatement().close();
			return result;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			model.lastError = "error ".concat(e.toString());
			return null;
		}
	}

	/**
	 * Task 2 and 3: Modify data (task 2) and (before) verify(task 3) it, and disable autocommit (task 4.1)
	 * USE: model.modifyData and Model.ModifyResult
	 * @param data Modify data
	 * @param AutoCommit autocommit parameter
	 * @param log Log container
	 * @return true if verify ok else false
	 */
	public boolean modifyData(Map data,boolean AutoCommit, List<String> log) {
		Model.ModifyResult result = Model.ModifyResult.Error;
		if(verifyData(data, log) == true) {
		result = model.modifyData(data, AutoCommit);
		}
		else{
			return false;
		}
		if(result == ModifyResult.Error){
			log.add(model.lastError);
			return false;
		}
		else if(result == ModifyResult.UpdateOccured){
			log.add("1 row updated");
			return true;
		}
			
		else{
			log.add("1 row insrted");
			//TODO Task 2,3,4.1
			return true;
		}
	

	}

	/**
	 * Task 5: get statistics
	 * USE: model.getStatistics
	 * Don't forget close the statement!
	 * @param log Log container
	 * @return every row in a String[],and the whole table in List<String[]>
	 */
	public List<String[]> getStatistics(List<String> log) {
		List<String[]> result = new ArrayList<>();
		String day[]={"Hetfo", "Kedd", "Szerda", "Csutortok",
				"Pentek", "Szombat", "Vasarnap", "Atlag"};
		double ossz=0;
	
		
		try{
		for(int i=1; i<8; i++){
			ResultSet rset = model.getStatistics(i);
			while(rset.next()){
				String str[] ={day[i-1], rset.getString(1)};
				result.add(str);
				ossz = ossz +rset.getInt(1);
			}
		}
		String str[] = {day[day.length-1], String.valueOf(ossz/7.0)};
		result.add(str);
		
		//TODO task 5
		return result;
		}
		catch(Exception e){
			log.add(e.toString());
			return null;
		}
	}

	/**
	 * Commit all uncommitted changes
	 * USE: model.commit
	 * @param log Log container
	 * @return true if model.commit true else false
	 */
	public boolean commit(List<String> log) {
		if (model.commit()) return true;
		log.add(model.lastError);
		return false;
	}

	/**
	 * Verify all fields value
	 * USE it to modifyData function
	 * USE regular expressions, try..catch
	 * @param data Modify data
	 * @param log Log container
	 * @return true if all fields in Map is correct else false
	 */
	private boolean verifyData(Map data, List<String> log) {
		boolean hibas=false;
		String[] mapkey ={"KOD", "TIPUS", "MUSZ_ELL", "JAVITAS", 
				"ULES", "HATOTAV"};
		String[] whereIsTheException = {"Kod error: 1-7 szamu numerikus adat", 
				"Tipus error: 1-7 szamu karakter", 
				"Muszaki Ellenorzes error: Nem megfelelo a datum formatuma (YYYY-MM-DD)",
				"Javitas error: Nem megfelelo a datum formatuma (YYYY-MM-DD)",
				"Ules error: A mezo ures volt, vagy nem szamot tartalmazott ezert az erteke default: 400",
				"Hatotav error: Nem lehet ures mezo (numerikus szamot var)"};
		String[] regularExpression= {"([0-9]{1,7})", "[0-9a-zA-Z]{1,7}", "[0-9]{4}-[0-1][0-9]-[0-3][0-9]",
				"[0-9]{4}-[0-1][0-9]-[0-3][0-9]", "[0-9]+", "[0-9]+"};
		int i=0;
		try {
			while(i < regularExpression.length){
				Pattern r = Pattern.compile(regularExpression[i]);
				Matcher m = r.matcher((String)data.get(mapkey[i]));
			
				if(!m.matches()){
					log.add(whereIsTheException[i]);
					if(mapkey[i]=="ULES"){
						data.replace("ULES", "400");
					}
					else{
					 hibas = true;
					}
				}
				i++;
			}
			if(hibas){
				return false;
			}
		}catch(Exception e) {
			log.add(e.toString());
			return false;
		}
		return true;
	}

}
