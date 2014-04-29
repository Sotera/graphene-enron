package graphene.enron.ingest;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;


public class EnronCreateTables {
	
	public void create()
	{

	  		Connection conn = App.getConnection();
	  		if (conn == null)
	  			return;
	  		
			boolean result = createTables(conn);
			System.out.println("Result of create tables " + result);
			
		
	}
	
	boolean createTables(Connection conn)
	{

		if (!createTransactions(conn))
			return false;
		
		if (!createEntityref(conn))
			return false;
		
		if (!createIdTypes(conn))
			return false;
		
		return true;
		
		
	}
	
	boolean createTransactions(Connection conn)
	{
		String trans = 
				"CREATE CACHED TABLE " + App.transactionstab + 
				"( " + 
						"pair_id int PRIMARY KEY NOT NULL," + 
						"id_search_token numeric(15, 0) NOT NULL," + 
						"sender_id numeric(15, 0) NOT NULL," +
						"sender_value_str varchar(200) NULL," +
						"receiver_id numeric(15, 0) NOT NULL," +
						"receiver_value_str varchar(200) NULL," +
						"trn_dt datetime NOT NULL," +
						"trn_type varchar(200) NULL," +
						"trn_value_nbr double," +
						"trn_value_nbr_unit varchar(200) NULL,"+
						"trn_value_str varchar(9000) NULL" +
					")"  ;

		Statement st;
		try {
			st = conn.createStatement();
			st.execute(trans);
			st.execute("CREATE INDEX send_idx on " +  App.transactionstab 
					+ "(sender_id)");
			st.execute("CREATE INDEX rcv_idx on " +  App.transactionstab 
					+ "(receiver_id)");
			st.execute("CREATE INDEX id_idx on " +  App.transactionstab 
					+ "(pair_id)");
			
		} catch (SQLException e3) {
			// TODO Auto-generated catch block
			e3.printStackTrace();
			return false;
		}
	
		System.out.println("Created transaction table");
		return true;
		
	}
	
	boolean createEntityref(Connection conn)
	{
String sql = "CREATE CACHED TABLE " + App.entityreftab + "(" +  
		"entityref_id int PRIMARY KEY NOT NULL," +
		"identifier varchar(800) NOT NULL," +
		"identifierColumnSource varchar(100) NOT NULL," +
		"identifierTableSource varchar(100) NOT NULL," +
		"accountNumber varchar(200) NULL," +
		"accountType varchar(10) NULL," +
		"dateStart datetime NULL," +
		"dateEnd datetime NULL," +
		"customerNumber varchar(30) NULL," +
		"customerType varchar(20) NULL," +
		"idtype_id int NULL" +
	") ";

		
		Statement st;
		try {
			st = conn.createStatement();
			st.execute(sql);
		} catch (SQLException e3) {
			// TODO Auto-generated catch block
			e3.printStackTrace();
			return false;
		}
		System.out.println("Created entity table");	
		return true;
		
		
		
	}
	
	boolean createIdTypes(Connection conn)
	{
String sql = "CREATE MEMORY TABLE " + App.typestab + "(" +

"	idtype_id int NOT NULL," +
"	columnSource varchar(120) NULL," +
"	tableSource varchar(120) NULL, " +
"	short_name varchar(120) NULL," +
"	family varchar(120) NULL" +
")";

		Statement st;
		try {
			st = conn.createStatement();
			st.execute(sql);
		} catch (SQLException e3) {
			// TODO Auto-generated catch block
			e3.printStackTrace();
			return false;
		}
		System.out.println("Created types table");
		return true;
		
		
		
	}
}
