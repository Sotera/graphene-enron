package graphene.enron.model;

import graphene.model.idl.G_SymbolConstants;
import graphene.util.db.DBConnectionPoolService;
import graphene.util.db.MainDB;
import graphene.util.db.SecondaryDB;

import org.apache.tapestry5.ioc.OrderedConfiguration;
import org.apache.tapestry5.ioc.annotations.Contribute;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Marker;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.ioc.internal.services.ClasspathResourceSymbolProvider;
import org.apache.tapestry5.ioc.services.SymbolProvider;
import org.apache.tapestry5.ioc.services.SymbolSource;
import org.slf4j.Logger;

/**
 * A module used specifically for the generation of DTO Objects. This may need
 * closer integration with the ingest modules. Note that right now this class
 * uses the same database.properties as the application does for running.
 * Potentially these could be different if the user you log in with can't read
 * the metadata needed by QueryDSL.
 * 
 * @author djue
 * 
 */
public class DTOGenerationModule {
	@Contribute(SymbolSource.class)
	public void contributePropertiesFileAsSymbols(
			OrderedConfiguration<SymbolProvider> configuration) {
		configuration.add("DatabaseConfiguration",
				new ClasspathResourceSymbolProvider("database.properties"),
				"after:SystemProperties", "before:ApplicationDefaults");
	}

	/**
	 * Note that when injecting a String symbol you must also add the @Inject
	 * annotation in addition to the @Symbol annotation.
	 * 
	 * 
	 * @param serverUrl
	 * @param userName
	 * @param userPassword
	 * @param logger
	 * @return
	 */
	@Marker(MainDB.class)
	public static DBConnectionPoolService buildMainDBConnectionPool(
			@Inject @Symbol(G_SymbolConstants.MIDTIER_SERVER_URL) String serverUrl,
			@Inject @Symbol(G_SymbolConstants.MIDTIER_SERVER_USERNAME) String userName,
			@Inject @Symbol(G_SymbolConstants.MIDTIER_SERVER_PASSWORD) String userPassword,
			final Logger logger) {
		try {
			return new DBConnectionPoolService(logger, serverUrl, userName,
					userPassword, true);
		} catch (Exception e) {
			logger.error(e.getMessage());
			return null;
		}
	}

	/**
	 * Builder for a second database, if needed. Note that when injecting a
	 * String symbol you must also add the @Inject annotation in addition to the @Symbol
	 * annotation.
	 * 
	 * 
	 * @param serverUrl
	 * @param userName
	 * @param userPassword
	 * @param logger
	 * @return
	 */
	@Marker(SecondaryDB.class)
	public static DBConnectionPoolService buildSecondaryConnectionPool(
			@Inject @Symbol(G_SymbolConstants.MIDTIER_SERVER_URL) String serverUrl,
			@Inject @Symbol(G_SymbolConstants.MIDTIER_SERVER_USERNAME) String userName,
			@Inject @Symbol(G_SymbolConstants.MIDTIER_SERVER_PASSWORD) String userPassword,
			final Logger logger) {
		try {
			return new DBConnectionPoolService(logger, serverUrl, userName,
					userPassword, true);
		} catch (Exception e) {
			logger.error(e.getMessage());
			return null;
		}
	}
}
