package graphene.enron.model.graphserver;

import graphene.services.EventGraphBuilder;
import graphene.services.PropertyGraphBuilder;

import org.apache.tapestry5.ioc.ServiceBinder;

public class GraphServerModule {
	public static void bind(ServiceBinder binder) {
		binder.bind(EventGraphBuilder.class, EventGraphBuilderEnronImpl.class)
				.withId("Event");

		binder.bind(PropertyGraphBuilder.class,
				PropertyGraphBuilderEnronImpl.class).withId("Property");

	}

}
