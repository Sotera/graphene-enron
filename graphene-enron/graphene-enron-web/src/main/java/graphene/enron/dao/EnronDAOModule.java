package graphene.enron.dao;

import graphene.dao.DataSourceListDAO;
import graphene.dao.EntityDAO;
import graphene.dao.EntityRefDAO;
import graphene.dao.IdTypeDAO;
import graphene.dao.TransactionDAO;
import graphene.enron.dao.impl.DataSourceListDAOImpl;
import graphene.enron.dao.impl.EntityDAOImpl;
import graphene.enron.dao.impl.EntityRefDAOImpl;
import graphene.enron.dao.impl.EntityRefSearch;
import graphene.enron.dao.impl.IdTypeDAOSQLImpl;
import graphene.enron.dao.impl.TransactionDAOSQLImpl;
import graphene.enron.model.memorydb.EnronMemoryDB;
import graphene.model.memorydb.IMemoryDB;
import graphene.util.PropertiesFileSymbolProvider;
import graphene.util.db.DBConnectionPoolService;
import graphene.util.db.MainDB;

import org.apache.tapestry5.ioc.Invokable;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.ScopeConstants;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Startup;
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
public class EnronDAOModule {

	private static Logger logger = LoggerFactory
			.getLogger(EnronDAOModule.class);

	public static void bind(ServiceBinder binder) {

		String tcHome = System.getenv("CATALINA_HOME");
		logger.debug("TC Home = " + tcHome);

		// FIXME: Remove this test once we get it working
		logger.debug("======== TESTING JDBC DRIVER TO SEE IF IT IS ON THE CLASSPATH");

		try {
			Class.forName("org.hsqldb.jdbc.JDBCDriver");
			logger.debug("+++++++++ SUCCESS org.hsqldb.jdbc.JDBCDriver");
		} catch (ClassNotFoundException e1) {
			logger.warn("======== Could not find org.hsqldb.jdbc.JDBCDriver on classpath");
			e1.printStackTrace();
		}

		// Note that despite the name, EntityRefDAOSQLImpl will use memdb
		// if available. Also note that currently it has to be available
		// since not all functionality has been written to support SQL

		binder.bind(EntityRefDAO.class, EntityRefDAOImpl.class).scope(
				ScopeConstants.PERTHREAD);
		// binder.bind(EntityRefDAO.class, EntityRefDAODiskImpl.class);
		// binder.bind(EntityDAO.class, EntityDAOMemImpl.class);

		binder.bind(EntityDAO.class, EntityDAOImpl.class);

		binder.bind(IdTypeDAO.class, IdTypeDAOSQLImpl.class);

		binder.bind(TransactionDAO.class, TransactionDAOSQLImpl.class).scope(
				ScopeConstants.PERTHREAD);
		/*
		 * binder.bind(TransferDAO.class, TransferDAOSQLImpl.class).scope(
		 * ScopeConstants.PERTHREAD);
		 */

//		binder.bind(TransactionDistinctPairDAO.class,
//				TransactionDistinctAccountPairDAOImpl.class).scope(
//				ScopeConstants.PERTHREAD);

		// TODO: Make this into a service in the core we can contribute to (for
		// distributed configuration!)
		binder.bind(DataSourceListDAO.class, DataSourceListDAOImpl.class);

		binder.bind(EntityRefSearch.class);

		binder.bind(IMemoryDB.class, EnronMemoryDB.class);
	}

	@Startup
	public static void registerHSQLDBShutdownHook(
			@Inject @MainDB final DBConnectionPoolService cps) {
		/**
		 * TODO: Find a way to contribute to a particular pool's shutdown hook.
		 * This is needed for embedded databases, where we have to restart the
		 * database along with the application and the pool.
		 * 
		 * The reason to make it a contribution is because we need to shutdown
		 * the database before we shutdown the connection pool. Research this
		 * more, and see if we want to handle this as a contribution or as an
		 * extension of the connection pool service.
		 */
		// Runtime.getRuntime().addShutdownHook(new Thread() {
		// @Override
		// public void run() {
		// if (cps != null) {
		// logger.info("Shuting down Connection Pool for "
		// + cps.getUrl() + " from registerHSQLDBShutdownHook");
		// Connection conn;
		// try {
		// conn = cps.getConnection();
		// java.sql.Statement statement = conn.createStatement();
		// statement.executeUpdate("SHUTDOWN");
		// statement.close();
		// conn.close();
		// } catch (Exception e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// JVMHelper.suggestGC();
		// }
		// }
		// });
	}

	// added for testing --djue
	public void contributeApplicationDefaults(
			MappedConfiguration<String, String> configuration) {
		configuration.add("MAX_MEMDB_ROWS_PARAMETER", "0");
		configuration.add("USE_MEMDB_PARAMETER", "true");
	}

	final static String MAX_MEMDB_ROWS_PARAMETER = "graphene.memorydb-maxIndexRecords";
	final static String USE_MEMDB_PARAMETER = "graphene.memorydb-useMemDB";

	@Startup
	public static void scheduleJobs(ParallelExecutor executor,
			final IMemoryDB memoryDb) {
		executor.invoke(IMemoryDB.class, new Invokable<IMemoryDB>() {
			@Override
			public IMemoryDB invoke() {
				memoryDb.initialize(0);
				return memoryDb;
			}
		});
	}

	//
	// @Startup
	// public static void initMemoryDb(Logger logger, EnronMemoryDB memoryDb,
	// @Inject @Symbol(USE_MEMDB_PARAMETER) String useMemoryDB,
	// @Inject @Symbol(MAX_MEMDB_ROWS_PARAMETER) String maxRecords) {
	//
	// logger.info("Initializing MemoryDB");
	// // String useMemoryDB = provider.valueForSymbol(USE_MEMDB_PARAMETER);
	//
	// if (useMemoryDB.equalsIgnoreCase("true")) {
	// int maxRecordsInt = 100000;
	// // String maxRecords = provider
	// // .valueForSymbol(MAX_MEMDB_ROWS_PARAMETER);
	// if (StringUtils.isNumeric(maxRecords)) {
	// maxRecordsInt = FastNumberUtils.parseIntWithCheck(maxRecords);
	// } else {
	// logger.error("The was an error reading "
	// + MAX_MEMDB_ROWS_PARAMETER
	// + ": The value was not an integer.");
	// logger.info("Using default " + MAX_MEMDB_ROWS_PARAMETER
	// + " value :" + maxRecordsInt);
	// }
	//
	// TimeReporter t = new TimeReporter("Initialize MemoryDB", logger);
	// logger.trace("Free memory before preload " + JVMHelper.getFreeMem());
	// memoryDb.initialize(maxRecordsInt);
	// logger.trace("Free memory after preload: " + JVMHelper.getFreeMem());
	// t.logAsCompleted();
	// }
	// }

	/**
	 * Note that any @Startup methods come before any @EagerLoad.
	 * 
	 * @param logger
	 * @param memoryDb
	 */
	// @Startup
	// public static void initMemoryDb(Logger logger, EnronMemoryDB memoryDb,
	// Context context) {
	//
	// final String MAX_MEMDB_ROWS_PARAMETER = "maxIndexRecords";
	// final String USE_MEMDB_PARAMETER = "useMemDB";
	//
	// logger.info("Initializing MemoryDB");
	// if (context.getInitParameter(USE_MEMDB_PARAMETER).equalsIgnoreCase(
	// "true")) {
	// int maxRecords = 100000;
	// try {
	// // Try to load from the web.xml
	// maxRecords = FastNumberUtils.parseIntWithCheck(context
	// .getInitParameter(MAX_MEMDB_ROWS_PARAMETER));
	// } catch (NumberFormatException e) {
	// logger.error("The was an error reading "
	// + MAX_MEMDB_ROWS_PARAMETER + ": " + e.getMessage());
	// logger.info("Using default " + MAX_MEMDB_ROWS_PARAMETER
	// + " value :" + maxRecords);
	// }
	//
	// TimeReporter t = new TimeReporter("Initialize MemoryDB", logger);
	// logger.trace("Free memory before preload " + JVMHelper.getFreeMem());
	// memoryDb.initialize(maxRecords);
	// logger.trace("Free memory after preload: " + JVMHelper.getFreeMem());
	// t.logAsCompleted();
	// }
	// }

	public PropertiesFileSymbolProvider buildTableNameSymbolProvider(
			Logger logger) {
		return new PropertiesFileSymbolProvider(logger,
				"tablenames.properties", true);
	}
}
