package graphene.enron.dao;

import graphene.dao.DataSourceListDAO;
import graphene.dao.EntityDAO;
import graphene.dao.EntityRefDAO;
import graphene.dao.IdTypeDAO;
import graphene.dao.PermissionDAO;
import graphene.dao.RoleDAO;
import graphene.dao.TransactionDAO;
import graphene.dao.neo4j.DAONeo4JEModule;
import graphene.dao.sql.DAOSQLModule;
import graphene.enron.dao.impl.DataSourceListDAOImpl;
import graphene.enron.dao.impl.EntityRefDAOImpl;
import graphene.enron.dao.impl.EntityRefSearch;
import graphene.enron.dao.impl.IdTypeDAOSQLImpl;
import graphene.enron.dao.impl.TransactionDAOSQLImpl;
import graphene.enron.model.memorydb.EnronMemoryDB;
import graphene.model.idl.G_SymbolConstants;
import graphene.model.memorydb.IMemoryDB;
import graphene.services.EntityDAOImpl;
import graphene.services.SimplePermissionDAOImpl;
import graphene.services.SimpleRoleDAOImpl;
import graphene.util.FastNumberUtils;
import graphene.util.PropertiesFileSymbolProvider;
import graphene.util.db.JDBCUtil;

import org.apache.tapestry5.ioc.Configuration;
import org.apache.tapestry5.ioc.Invokable;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.ScopeConstants;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.annotations.Contribute;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Startup;
import org.apache.tapestry5.ioc.annotations.SubModule;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.ioc.services.ParallelExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Map the interfaces to the implementations you want to use. By default these
 * are singletons.
 * 
 * @author djue
 * 
 */
@SubModule({ DAOSQLModule.class, DAONeo4JEModule.class })
public class EnronDAOModule {

	private static Logger logger = LoggerFactory
			.getLogger(EnronDAOModule.class);

	public static void bind(ServiceBinder binder) {
		
		
		binder.bind(RoleDAO.class, SimpleRoleDAOImpl.class);
		binder.bind(PermissionDAO.class, SimplePermissionDAOImpl.class);
		
		
		
		binder.bind(EntityRefDAO.class, EntityRefDAOImpl.class).scope(
				ScopeConstants.PERTHREAD);

		binder.bind(EntityDAO.class, EntityDAOImpl.class);

		binder.bind(IdTypeDAO.class, IdTypeDAOSQLImpl.class);

		binder.bind(TransactionDAO.class, TransactionDAOSQLImpl.class).scope(
				ScopeConstants.PERTHREAD);

		// TODO: Make this into a service in the core we can contribute to (for
		// distributed configuration!)
		binder.bind(DataSourceListDAO.class, DataSourceListDAOImpl.class);

		binder.bind(EntityRefSearch.class);

		binder.bind(IMemoryDB.class, EnronMemoryDB.class);
	}
	final static String MAX_MEMDB_ROWS_PARAMETER = "graphene.memorydb-maxIndexRecords";
	final static String USE_MEMDB_PARAMETER = "graphene.memorydb-useMemDB";
	// added for testing --djue
	public void contributeApplicationDefaults(
			MappedConfiguration<String, String> configuration) {
		configuration.add(MAX_MEMDB_ROWS_PARAMETER, "0");
		configuration.add(USE_MEMDB_PARAMETER, "true");
		configuration.add(G_SymbolConstants.CACHEFILELOCATION, "%CATALINA_HOME%/data/EnronEntityRefCache.data");
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

	@Startup
	public static void scheduleJobs(ParallelExecutor executor,
			final IMemoryDB memoryDb,
			@Inject @Symbol(USE_MEMDB_PARAMETER) final String useMemoryDB,
			@Inject @Symbol(MAX_MEMDB_ROWS_PARAMETER) final String maxRecords) {

		System.out.println(USE_MEMDB_PARAMETER + "=" + useMemoryDB);
		System.out.println(MAX_MEMDB_ROWS_PARAMETER + "=" + maxRecords);
		if ("true".equalsIgnoreCase(useMemoryDB)) {
			System.out
					.println("Scheduling parallel job to load in-memory database.");
			executor.invoke(IMemoryDB.class, new Invokable<IMemoryDB>() {
				@Override
				public IMemoryDB invoke() {
					memoryDb.initialize(FastNumberUtils
							.parseIntWithCheck(maxRecords));
					return memoryDb;
				}
			});
		}
	}

	public PropertiesFileSymbolProvider buildTableNameSymbolProvider(
			Logger logger) {
		return new PropertiesFileSymbolProvider(logger,
				"tablenames.properties", true);
	}
}
