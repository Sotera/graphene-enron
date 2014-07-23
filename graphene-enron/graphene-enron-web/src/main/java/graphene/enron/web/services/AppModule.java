package graphene.enron.web.services;

import graphene.enron.dao.EnronDAOModule;
import graphene.enron.model.graphserver.GraphServerModule;
import graphene.model.idl.G_SymbolConstants;
import graphene.rest.services.RestModule;
import graphene.util.PropertiesFileSymbolProvider;
import graphene.util.UtilModule;
//import graphene.web.security.ShiroSecurityModule;
import graphene.web.security.NoSecurityModule;
import graphene.web.services.GrapheneModule;

import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.OrderedConfiguration;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.annotations.InjectService;
import org.apache.tapestry5.ioc.annotations.SubModule;
import org.apache.tapestry5.ioc.services.SymbolProvider;
import org.slf4j.Logger;
import org.tynamo.security.SecuritySymbols;

/**
 * This module is automatically included as part of the Tapestry IoC Registry,
 * it's a good place to configure and extend Tapestry, or to place your own
 * service definitions.
 */
@SubModule({ EnronDAOModule.class, AppRestModule.class,
		GraphServerModule.class, GrapheneModule.class, RestModule.class,
		UtilModule.class, NoSecurityModule.class })
public class AppModule {

	public static void bind(ServiceBinder binder) {
		// binder.bind(MyServiceInterface.class, MyServiceImpl.class);

		// Make bind() calls on the binder object to define most IoC services.
		// Use service builder methods (example below) when the implementation
		// is provided inline, or requires more initialization than simply
		// invoking the constructor.

	}

	public static void contributeApplicationDefaults(
			MappedConfiguration<String, Object> configuration) {
		configuration.add(G_SymbolConstants.APPLICATION_NAME, "Graphene-Enron");
		configuration.add(G_SymbolConstants.APPLICATION_CONTACT,
				"Example Company");
		configuration.override(SecuritySymbols.SUCCESS_URL, "/index");
		configuration.add(SymbolConstants.APPLICATION_VERSION, "4.0.8");
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
