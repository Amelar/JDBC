/**
 * This JavaFX skeleton is provided for the Software Laboratory 5 course. Its structure
 * should provide a general guideline for the students.
 * As suggested by the JavaFX model, we'll have a GUI (view),
 * a controller class and a model (this one).
 */

package application;

import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Map;

// Model class
public class Model {

	// Database driver and URL
	protected static final String driverName = "oracle.jdbc.driver.OracleDriver";
	protected static final String databaseUrl = "jdbc:oracle:thin:@rapid.eik.bme.hu:1521:szglab";

	// Product name and product version of the database
	protected String databaseProductName = null;
	protected String databaseProductVersion = null;

	// Connection object
	protected Connection connection = null;

	// Enum structure for Exercise #2
	protected enum ModifyResult {
		InsertOccured, UpdateOccured, Error
	}

	// String containing last error message
	protected String lastError = "";

	/**
	 * Model constructor
	 */
	public Model() {
	}
	


	/**
	 * Gives product name of the database
	 *
	 * @return Product name of the database
	 */
	public String getDatabaseProductName() {

		return databaseProductName;

	}

	/**
	 * Gives product version of the database
	 *
	 * @return Product version of the database
	 */
	public String getDatabaseProductVersion() {

		return databaseProductVersion;

	}

	/**
	 * Gives database URL
	 *
	 * @return Database URL
	 */
	public String getDatabaseUrl() {

		return databaseUrl;

	}

	/**
	 * Gives the message of last error
	 *
	 * @return Message of last error
	 */
	public String getLastError() {

		return lastError;

	}

	/**
	 * Tries to connect to the database
	 *
	 * @param userName
	 *            User who has access to the database
	 * @param password
	 *            User's password
	 * @return True on success, false on fail
	 */
	public boolean connect(String userName, String password) {

		try {

			// If connection status is disconnected
			if (connection == null || !connection.isValid(30)) {

				if (connection == null) {

					// Load the specified database driver
					Class.forName(driverName);

					// Driver is for Oracle 12cR1 (certified with JDK 7 and JDK
					// 8)
					if (java.lang.System.getProperty("java.vendor").equals("Microsoft Corp.")) {
						DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());
					}
				} else {
					connection.close();

				}

				// Create new connection and get metadata
				connection = DriverManager.getConnection(databaseUrl, userName, password);
				DatabaseMetaData dbmd = connection.getMetaData();

				databaseProductName = dbmd.getDatabaseProductName();
				databaseProductVersion = dbmd.getDatabaseProductVersion();

			}

			return true;

		} catch (SQLException e) {

			// !TODO: More user friendly error handling
			// use 'error' String beginning of the error string
			lastError = "error ".concat(e.toString());
			return false;

		} catch (ClassNotFoundException e) {
			// !TODO: More user friendly error handling
			// use 'error' String beginning of the error string
			lastError = "error ".concat(e.toString());
			return false;

		}


	}

	/**
	 * Tests the database connection by submitting a query
	 *
	 * @return True on success, false on fail
	 */
	public String testConnection() {

		try {

			// Create SQL query and execute it
			// If user input has to be processed, use PreparedStatement instead!
			Statement stmt = connection.createStatement();
			ResultSet rset = stmt.executeQuery("SELECT count(*) FROM oktatas.igazolvanyok");

			// Process the results
			String result = null;
			while (rset.next()) {
				result = String.format("Total number of rows in 'Igazolvanyok' table in 'Oktatas' schema: %s",
						rset.getString(1));
			}

			// Close statement
			stmt.close();

			return result;

		} catch (SQLException e) {
			// !TODO: More user friendly error handling
			// use 'error' String beginning of the error string
			lastError = "error ".concat(e.toString());
			return null;

		}
	}
	/**
	 * Method for Exercise 
	 * @return Result of the query
	 */
	public ResultSet listAll() {
		try {
			Statement stmt = connection.createStatement();
			ResultSet rset = stmt.executeQuery("SELECT szam, ind, erk FROM jarat");
			
			return rset;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			lastError = "error ".concat(e.toString());
			return null;
		}
		
	}

	/**
	 * Method for Exercise #1
	 * @param Search keyword
	 * @return Result of the query
	 */
	public ResultSet search(String keyword) {
		String selectSql= "SELECT szam, ind, erk FROM jarat "+
				"WHERE honnan LIKE ? OR hova LIKE ? ";
		
		try {
			PreparedStatement pstmt = 
					connection.prepareStatement(selectSql);
			
			String newkeyword =  keyword
					.replace("%", "!%")
					.replace("_", "!_");
			newkeyword = newkeyword + "%";
			pstmt.setString(1, newkeyword);
			pstmt.setString(2, newkeyword);
			ResultSet rset = pstmt.executeQuery();
			return rset;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			lastError = "error ".concat(e.toString());
			return null;
		}
	}

	/**
	 * Method for Exercise #2-#3
	 *
	 * @param data
	 *            New or modified data
	 * @param AutoCommit set the connection type (use default true, and 4.1 use false
	 * @return Type of action has been performed
	 */
	public ModifyResult modifyData(Map data, boolean AutoCommit) {

		ModifyResult result = ModifyResult.Error;
		String SQLCodeIsExist ="SELECT kod FROM gep " +
				"WHERE kod = ?";
		
		String sqlInsert = "INSERT INTO gep (kod, tipus, musz_ell, javitas, "+ 
				"legcsavaros, ules, hatotav, megjegyzes) "+
				"VALUES(?, ?, ?, ?, ?, ?, ?, ?)";
		
		String sqlUpdate = "UPDATE gep "+
				"SET tipus = ?, musz_ell = ?, javitas = ?, legcsavaros = ?, " +
				"ules = ?, hatotav = ?, megjegyzes =? WHERE kod = ?";
		
		String sqlMenetrend = "INSERT INTO menetrend (ID, kod, szam, nap) " +
				"VALUES(sorszam.nextval, ?, ?, 1)";
		

		try {	
			connection.setAutoCommit(AutoCommit);
			PreparedStatement CodeIsExist = connection.prepareStatement(SQLCodeIsExist);
			CodeIsExist.setString(1, (String)data.get("KOD"));
			
			PreparedStatement pstmt;
			ResultSet rset =(CodeIsExist.executeQuery());
			rset.next();
			if(rset.getRow()==0){
				pstmt = connection.prepareStatement(sqlInsert);
				pstmt.setString(1, (String)data.get("KOD"));
				pstmt.setString(2, (String)data.get("TIPUS"));
				String DateFormat = "yyyy-MM-dd";
				SimpleDateFormat sdf = new SimpleDateFormat(DateFormat);
				java.util.Date date = sdf.parse((String)(data.get("MUSZ_ELL")));
				java.util.Date date2 = sdf.parse((String)data.get("JAVITAS"));
				pstmt.setDate(3, new java.sql.Date(date.getTime()));
				pstmt.setDate(4, new java.sql.Date(date2.getTime()));
					
				if(data.get("LEGCSAVAROS")=="Y"){
					pstmt.setInt(5, 1);
				}
				else{
					pstmt.setInt(5, 0);
				}
					
				pstmt.setString(6, (String)data.get("ULES"));
				pstmt.setString(7, (String)data.get("HATOTAV"));
				pstmt.setString(8, (String)data.get("MEGJEGYZES"));
				pstmt.executeUpdate();
					
				result = ModifyResult.InsertOccured;
				
				pstmt = connection.prepareStatement(sqlMenetrend);
				
				pstmt.setString(1, (String)data.get("KOD"));
				pstmt.setString(2, (String)data.get("JARATSZAM"));
				pstmt.executeUpdate();
			}
			else{
				pstmt = connection.prepareStatement(sqlUpdate);
				pstmt.setString(1, (String)data.get("TIPUS"));
				String DateFormat = "yyyy-MM-dd";
				SimpleDateFormat sdf = new SimpleDateFormat(DateFormat);
				java.util.Date date = sdf.parse((String)(data.get("MUSZ_ELL")));
				java.util.Date date2 = sdf.parse((String)data.get("JAVITAS"));
				pstmt.setDate(2, new java.sql.Date(date.getTime()));
				pstmt.setDate(3, new java.sql.Date(date2.getTime()));
					
				if(data.get("LEGCSAVAROS")=="Y"){
					pstmt.setInt(4, 1);
				}
				else{
					pstmt.setInt(4, 0);
				}
				pstmt.setString(6, (String)data.get("ULES"));
				pstmt.setString(6, (String)data.get("HATOTAV"));
				pstmt.setString(7, (String)data.get("MEGJEGYZES"));
				pstmt.setString(8, (String)data.get("KOD"));
				pstmt.executeUpdate();
					
				result = ModifyResult.UpdateOccured;
				
			pstmt = connection.prepareStatement(sqlMenetrend);
			
			pstmt.setString(1, (String)data.get("KOD"));
			pstmt.setString(2, (String)data.get("JARATSZAM"));
			pstmt.executeUpdate();
			}
			CodeIsExist.close();
			
			pstmt.close();
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				lastError = "error ".concat(e.toString());
				return result = ModifyResult.Error;
			}
			catch (SQLException e) {
				// TODO Auto-generated catch block
				lastError = "error ".concat(e.toString());
				rollback();
				return result = ModifyResult.Error;
			}
			catch (Exception e){
				lastError = "error ".concat(e.toString());
				return result = ModifyResult.Error;
			}

		
		//TODO task 2,3,4.1
		return result;

	}


	/**
	 * Method for Exercise #4
	 *
	 * @return True on success, false on fail
	 */
	public boolean commit() {
		try {
			connection.commit();
		} catch (SQLException e) {
			lastError = "error ".concat(e.toString());
			rollback();
			return false;
		}

		//TODO task 4
		return true;
	}

	/**
	 * Method for Exercise #4
	 */
	public void rollback(){
		try {
			connection.rollback();
		} catch (SQLException e) {
			lastError = "error ".concat(e.toString());
		}
	}

	/**
	 * Method for Exercise #5
	 *
	 * @return Result of the query
	 */
	public ResultSet getStatistics(int bit) {
		String sql = "select count(*) from jarat a,  menetrend b "+
				"where a.szam=b.szam and a.honnan='PRG' and (exists ( " +
				"select * from jarat, menetrend  where jarat.szam = menetrend.szam and hova='HEL' " +
				"and a.hova=jarat.honnan and "+ 
				"(bitand(b.nap,?)=bitand(menetrend.nap,?) or " +
				"a.hova = 'HEL')))";
		try {
			PreparedStatement pstmt = connection.prepareStatement(sql);
			ResultSet rset;
			pstmt.setInt(1, bit);
			pstmt.setInt(2, bit);
			rset = pstmt.executeQuery();
				
			return rset;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			lastError = "error ".concat(e.toString());
			return null;
		}

		

	}
	



}

