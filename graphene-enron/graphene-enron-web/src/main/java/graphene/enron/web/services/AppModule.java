package graphene.enron.web.services;

import graphene.enron.dao.EnronDAOModule;
import graphene.enron.model.graphserver.GraphServerModule;
import graphene.model.idl.G_SymbolConstants;
import graphene.util.UtilModule;

import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.annotations.Startup;
import org.apache.tapestry5.ioc.annotations.SubModule;
import org.apache.tapestry5.services.Context;
import org.slf4j.Logger;

/**
 * This module is automatically included as part of the Tapestry IoC Registry,
 * it's a good place to configure and extend Tapestry, or to place your own
 * service definitions.
 */
@SubModule({ EnronDAOModule.class, AppRestModule.class, GraphServerModule.class,
		graphene.web.services.GrapheneModule.class,
		graphene.rest.services.RestModule.class,UtilModule.class })
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
		configuration.add(G_SymbolConstants.APPLICATION_CONTACT, "Example Company");
		configuration.add(SymbolConstants.APPLICATION_VERSION, "4.0.6");
	}

	@Startup
	public static void readContext(Logger l, Context c) {
		Integer obj = (Integer) c.getAttribute("maxExemptions");
		l.debug("maxExemptions" + obj);
		String s = c.getInitParameter("asdf");
		l.debug(s);
	}
}
