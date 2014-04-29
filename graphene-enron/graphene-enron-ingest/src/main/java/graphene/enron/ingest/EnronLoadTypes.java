package graphene.enron.ingest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class EnronLoadTypes {

	public void load() throws SQLException
	{
		  Connection conn = App.getConnection();
		  PreparedStatement ps = null;
		  
		  String sql="INSERT INTO " + App.typestab;
		  sql += " (idtype_id,columnSource,tableSource,short_name,family)";
		  sql += "VALUES ?,?,?,?,?";
		 ps = conn.prepareStatement(sql);
		 
		if (ps == null)
			return;
		
		ps.setInt(1, 1);
		ps.setString(2, "Name");
		ps.setString(3, "email_graph");
		ps.setString(4, "Name");		
		ps.setString(5, "name");
		ps.execute();
		
		ps.setInt(1, 2);
		ps.setString(2, "Email");
		ps.setString(3, "email_graph");		
		ps.setString(4, "Email");
		ps.setString(5, "email");
		ps.execute();
		System.out.println("Loaded types");
	

	}
}
