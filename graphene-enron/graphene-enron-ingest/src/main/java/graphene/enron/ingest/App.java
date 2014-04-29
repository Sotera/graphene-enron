package graphene.enron.ingest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * App to ingest a subset of the Enron data from CSV files as a demo
 * 
 * Using static globals for simplicity - this was a one-off app developed under
 * a deadline.
 */

public class App {
	public static String rawpath = "J:/Projects/data/xdata/Enron";// "C:/data/Enron";
	public static String dbpath = "T:/BigData/Tomcat/enron/enrondb";// "T:/Tomcat/enron/enrondb";
	public static String DBUrl = "";

	public static String entityreftab = "ENRON_ENTITYREF_1_00";
	public static String transactionstab = "ENRON_TRANSACTION_PAIR_1_00";
	public static String typestab = "ENRON_IDENTIFIER_TYPE_1_00";

	public static Map<String, Integer> emailAddresses = new HashMap<String, Integer>();
	public static int nbr_addresses = 0;

	private static Connection conn = null;

	public static int eId = 0; // entityref Id number

	public static void main(String[] args) {
		try {
			Class.forName("org.hsqldb.jdbc.JDBCDriver");
			System.out.println("+++++++++ SUCCESS org.hsqldb.jdbc.JDBCDriver");
		} catch (ClassNotFoundException e1) {
			System.out
					.println("======== Could not find org.hsqldb.jdbc.JDBCDriver on classpath");
			e1.printStackTrace();
		}

		DBUrl = "jdbc:hsqldb:file:" + dbpath
				+ ";user=graphene;password=graphene";
		new EnronCreateTables().create();

		try {
			new EnronLoadTypes().load();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		}
		new EnronLoadEmailAddresses().load(rawpath);

		System.out.println("Found " + emailAddresses.size() + " addresses");

		new EnronLoadEntities().load();

		new EnronLoadEmails().load(rawpath);

		closeConnection();

	}

	public static Connection getConnection() {
		if (conn != null)
			return conn;

		System.out.println("Opening database connection ");
		try {
			conn = DriverManager.getConnection(DBUrl);
		} catch (SQLException eConn) {
			System.out.println(eConn.getMessage());
			System.exit(-1);
		}
		System.out.println("Opened database");
		return conn;

	}

	public static void closeConnection() {
		if (conn == null)
			return;

		try {
			conn.createStatement().execute("COMMIT");
			conn.createStatement().execute("SHUTDOWN");
		} catch (SQLException e2) {
			System.out.println("Could not commit " + e2.getMessage());
		}
		conn = null;

	}
}
