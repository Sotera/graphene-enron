package graphene.enron.web.services;

import graphene.dao.DataSourceListDAO;
import graphene.dao.EntityDAO;
import graphene.dao.EntityRefDAO;
import graphene.dao.IdTypeDAO;
import graphene.dao.PermissionDAO;
import graphene.dao.RoleDAO;
import graphene.dao.TransactionDAO;
import graphene.dao.sql.DAOSQLModule;
import graphene.enron.dao.impl.DataSourceListDAOImpl;
import graphene.enron.dao.impl.EntityRefDAOImpl;
import graphene.enron.dao.impl.IdTypeDAOSQLImpl;
import graphene.enron.dao.impl.TransactionDAOSQLImpl;
import graphene.enron.model.graphserver.GraphServerModule;
import graphene.enron.model.memorydb.EnronMemoryDB;
import graphene.model.idl.G_SymbolConstants;
import graphene.model.memorydb.IMemoryDB;
import graphene.services.EntityDAOImpl;
import graphene.services.SimplePermissionDAOImpl;
import graphene.services.SimpleRoleDAOImpl;
import graphene.util.PropertiesFileSymbolProvider;
import graphene.util.UtilModule;
import graphene.util.db.JDBCUtil;

import org.apache.tapestry5.ioc.Configuration;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.OrderedConfiguration;
import org.apache.tapestry5.ioc.ScopeConstants;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.annotations.Contribute;
import org.apache.tapestry5.ioc.annotations.InjectService;
import org.apache.tapestry5.ioc.annotations.SubModule;
import org.apache.tapestry5.ioc.services.SymbolProvider;
import org.slf4j.Logger;

@SubModule({ GraphServerModule.class,DAOSQLModule.class, UtilModule.class })
public class TestModule {

	public static void bind(ServiceBinder binder) {

		binder.bind(RoleDAO.class, SimpleRoleDAOImpl.class);
		binder.bind(PermissionDAO.class, SimplePermissionDAOImpl.class);

		binder.bind(EntityRefDAO.class, EntityRefDAOImpl.class).scope(
				ScopeConstants.PERTHREAD);

		binder.bind(EntityDAO.class, EntityDAOImpl.class);

		binder.bind(IdTypeDAO.class, IdTypeDAOSQLImpl.class);

		binder.bind(TransactionDAO.class, TransactionDAOSQLImpl.class).withId(
				"Primary");

		// TODO: Make this into a service in the core we can contribute to (for
		// distributed configuration!)
		binder.bind(DataSourceListDAO.class, DataSourceListDAOImpl.class);

		binder.bind(IMemoryDB.class, EnronMemoryDB.class);
	}

	final static String MAX_MEMDB_ROWS_PARAMETER = "graphene.memorydb-maxIndexRecords";
	final static String USE_MEMDB_PARAMETER = "graphene.memorydb-useMemDB";

	// added for testing --djue
	public void contributeApplicationDefaults(
			MappedConfiguration<String, String> configuration) {
		configuration.add(MAX_MEMDB_ROWS_PARAMETER, "0");
		configuration.add(USE_MEMDB_PARAMETER, "true");
		configuration.add(G_SymbolConstants.CACHEFILELOCATION,
				"%CATALINA_HOME%/data/EnronEntityRefCache.data");
	}

	/**
	 * Use this contribution to list the preferred drivers you would like to be
	 * used. Note that the jar files still need to be on the classpath, for
	 * instance in the Tomcat/lib directory or elsewhere.
	 * 
	 * @param configuration
	 */
	@Contribute(JDBCUtil.class)
	public static void contributeDesiredJDBCDriverClasses(
			Configuration<String> configuration) {
		configuration.add("org.hsqldb.jdbc.JDBCDriver");
	}


	public PropertiesFileSymbolProvider buildColorsSymbolProvider(Logger logger) {
		return new PropertiesFileSymbolProvider(logger,
				"graphene_optional_colors01.properties", true);
	}

	public static void contributeSymbolSource(
			OrderedConfiguration<SymbolProvider> configuration,
			@InjectService("ColorsSymbolProvider") SymbolProvider c) {
		configuration.add("ColorsPropertiesFile", c, "after:SystemProperties",
				"before:ApplicationDefaults");
	}
}
