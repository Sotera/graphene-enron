package graphene.enron.ingest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Class to create an entityref from the Email Addresses hashmap
 * 
 * @author pgofton
 * 
 */
public class EnronLoadEntities {

	PreparedStatement ps = null;

	public void load() {
		Connection conn = App.getConnection();

		try {
			conn.createStatement().execute("DELETE FROM " + App.entityreftab);
		} catch (SQLException e2) {
			System.out.println("Could not delete " + e2.getMessage());
			return;
		}

		String sql = "INSERT INTO "
				+ App.entityreftab
				+ "(customerNumber, AccountNumber, entityref_Id, identifier, identifierTableSource,identifierColumnSource,idtype_Id)"
				+ "VALUES ?,?,?,?,?,?,?";

		System.out.println("Preparing statement: " + sql);

		try {
			ps = conn.prepareStatement(sql);
		} catch (SQLException e1) {
			System.out.println("Prepare statement failed " + e1.getMessage());
			return;
		}

		for (String emailaddr : App.emailAddresses.keySet()) {
			Integer id = App.emailAddresses.get(emailaddr);

			try {
				parseEmail(emailaddr, id);
			} catch (SQLException e) {
				System.out.println(emailaddr);
				System.out.println("SQL Exception " + e.getMessage());
				return;
			}

		}

		System.out.println("Populated entityref with " + App.eId + " Entries");

	}

	public static final String tableSource = "EnronGraph.txt";

	private void parseEmail(String email, Integer id) throws SQLException {
		String name = emailToName(email);
		if (name == null)
			return;
		// We have two identifiers we can create from this data: the email
		// address and a reconstructed name.

		// The name we reconstructed.
		ps.setInt(1, id); // customernumber equiv
		ps.setString(2, email); // use email address as the id
		ps.setInt(3, App.eId++); // primary key for table
		ps.setString(4, name);// identifier

		ps.setString(5, tableSource);
		ps.setString(6, "Name");// identifiercolumnsource
		ps.setInt(7, 1); // idtype
		ps.execute();

		// Just the email address
		ps.setInt(1, id); // customernumber equiv
		ps.setString(2, email);
		ps.setInt(3, App.eId++); // primary key for table
		ps.setString(4, email);// identifier
		ps.setString(5, tableSource);
		ps.setString(6, "Email");// identifiercolumnsource
		ps.setInt(7, 2); // idtype
		ps.execute();

	}

	/**
	 * TODO: Extract just the part before '@' if the name parsing fails.
	 * 
	 * TODO: Choose the HTS version of this.
	 * 
	 * @param email
	 * @return
	 */
	private String emailToName(String email) {
		int atpos = email.indexOf("@");
		if (atpos == -1)
			return null;
		String name = email.substring(0, atpos);

		int dotpos = name.indexOf(".");
		if (dotpos != -1) {
			try {
				String first = name.substring(0, dotpos);
				String last = name.substring(dotpos + 1);
				name = nameFix(first) + " " + nameFix(last);
				name = name.trim();
			} catch (Exception e) {
				System.out.println("Error fixing name " + name);
				return null;
			}
			;
		}
		return name;
	}

	private String nameFix(String name) {
		if (name == null || name.length() == 0)
			return name;
		else
			return ("" + name.charAt(0)).toUpperCase() + name.substring(1);
	}
}
