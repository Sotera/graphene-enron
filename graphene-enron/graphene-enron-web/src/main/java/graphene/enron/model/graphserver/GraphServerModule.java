package graphene.enron.model.graphserver;

import mil.darpa.vande.interactions.InteractionFinder;
import mil.darpa.vande.interactions.InteractionGraphBuilder;
import mil.darpa.vande.property.PropertyFinder;
import mil.darpa.vande.property.PropertyGraphBuilder;

import org.apache.tapestry5.ioc.ServiceBinder;

public class GraphServerModule {
	public static void bind(ServiceBinder binder) {

		binder.bind(PropertyFinder.class, PropertyFinderEnronImpl.class).withId(
				"Property");
		binder.bind(InteractionFinder.class, InteractionFinderEnronImpl.class).withId(
				"Interaction");
		binder.bind(InteractionGraphBuilder.class);
		binder.bind(PropertyGraphBuilder.class);
//		binder.bind(GraphBuilder.class, GraphBuilderDirected.class)
//				.withId("Directed");
//
//		binder.bind(GraphBuilder.class,
//				GraphBuilderTransferImpl.class).withId("Transfer");

	}

}
