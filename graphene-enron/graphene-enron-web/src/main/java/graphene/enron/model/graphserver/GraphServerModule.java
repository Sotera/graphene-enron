package graphene.enron.model.graphserver;

import graphene.services.PropertyGraphBuilder;
import mil.darpa.vande.interactions.InteractionFinder;
import mil.darpa.vande.interactions.InteractionGraphBuilder;

import org.apache.tapestry5.ioc.ServiceBinder;

public class GraphServerModule {
	public static void bind(ServiceBinder binder) {

		binder.bind(InteractionFinder.class, InteractionFinderEnronImpl.class)
				.withId("Interaction");
		binder.bind(InteractionGraphBuilder.class);
		binder.bind(PropertyGraphBuilder.class, PropertyGraphBuilderImpl.class)
				.withId("Property");
		
	}

}
