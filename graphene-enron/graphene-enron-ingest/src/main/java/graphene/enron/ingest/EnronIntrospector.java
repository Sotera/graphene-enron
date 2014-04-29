package graphene.enron.ingest;

import graphene.enron.model.DTOGenerationModule;
import graphene.introspect.Introspector;
import graphene.util.ConnectionPoolModule;
import graphene.util.db.DBConnectionPoolService;
import graphene.util.db.MainDB;

import java.io.File;
import java.io.Serializable;
import java.sql.SQLException;

import org.apache.tapestry5.ioc.Registry;
import org.apache.tapestry5.ioc.RegistryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mysema.query.codegen.BeanSerializer;
import com.mysema.query.sql.HSQLDBTemplates;
import com.mysema.query.sql.codegen.MetaDataExporter;

/**
 * 
 * @author djue
 * 
 */
public class EnronIntrospector {

	private static Logger logger = LoggerFactory
			.getLogger(EnronIntrospector.class);
	private static final String namespace = "enron";
	private static DBConnectionPoolService mainDB;

	private static Registry registry;

	/**
	 * Generate DTO java classes for the tables matched by the regex you provide
	 * in the main.
	 */
	public static void generateDTO(DBConnectionPoolService cp,
			String tablePrefix, String packageName) {
		java.sql.Connection conn = null;
		try {

			conn = cp.getConnection();

			MetaDataExporter exporter = new MetaDataExporter();
			exporter.setPackageName(packageName);
			// exporter.setSchemaPattern("");

			exporter.setTargetFolder(new File("src/main/java"));

			// here we set up this object that will be applied to all beans
			// (DTOs)
			BeanSerializer bs = new BeanSerializer();
			// Here we are telling it to add the toString() method to each bean
			bs.setAddToString(true);
			// Here we are telling it to add 'implements Serializable' to each
			// bean. (no serializable id though, so you may see a warning.)
			bs.addInterface(Serializable.class);
			// then we give the exporter the BeanSerializer
			exporter.setBeanSerializer(bs);

			// example, get all the tables/views that match "foo*"
			exporter.setTableNamePattern(tablePrefix);
			// If you want views as well, change this to true.
			exporter.setExportViews(false);

			// This gets the metadata from the database and then starts creating
			// code for you.
			exporter.export(conn.getMetaData());
			conn.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if (conn != null && !conn.isClosed()) {
					conn.close();
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) throws Exception {
		setup();
		generateDTO(mainDB, null, "graphene."+namespace+".introspect.output");
		Introspector s = new Introspector(mainDB, new HSQLDBTemplates());
		s.setBeanPackageName("graphene."+namespace+".introspect.output");
		s.introspect();
	}

	public static void setup() {

		RegistryBuilder builder = new RegistryBuilder();
		builder.add(ConnectionPoolModule.class, DTOGenerationModule.class);
		registry = builder.build();
		registry.performRegistryStartup();
		mainDB = registry.getService(DBConnectionPoolService.class,
				MainDB.class);

	}

}
