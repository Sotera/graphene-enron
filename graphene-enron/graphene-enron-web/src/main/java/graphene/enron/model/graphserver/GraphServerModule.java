package graphene.enron.model.graphserver;

import mil.darpa.vande.legacy.graphserver.GraphBuilder;
import mil.darpa.vande.legacy.graphserver.GraphBuilderDirected;
import mil.darpa.vande.legacy.graphserver.GraphBuilderWithDirection;

import org.apache.tapestry5.ioc.ServiceBinder;

public class GraphServerModule {
	public static void bind(ServiceBinder binder) {

		binder.bind(GraphBuilder.class, GraphBuilderEntityImpl.class).withId(
				"Entity");

		binder.bind(GraphBuilderWithDirection.class, GraphBuilderDirected.class)
				.withId("Directed");

		binder.bind(GraphBuilderWithDirection.class,
				GraphBuilderTransferImpl.class).withId("Transfer");

	}

}
