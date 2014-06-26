package graphene.enron.web.services;

import graphene.enron.dao.EnronDAOModule;
import graphene.enron.model.graphserver.GraphServerModule;
import graphene.model.idl.G_SymbolConstants;
import graphene.rest.services.RestModule;
import graphene.util.UtilModule;
import graphene.web.security.ShiroSecurityModule;
import graphene.web.services.GrapheneModule;

import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.annotations.SubModule;

/**
 * This module is automatically included as part of the Tapestry IoC Registry,
 * it's a good place to configure and extend Tapestry, or to place your own
 * service definitions.
 */
@SubModule({ EnronDAOModule.class, AppRestModule.class,
		GraphServerModule.class, GrapheneModule.class, RestModule.class,
		UtilModule.class, ShiroSecurityModule.class })
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
		configuration.add(SymbolConstants.APPLICATION_VERSION, "4.0.8");
	}

}
