package graphene.enron.model.graphserver;

import graphene.model.graphserver.GraphBuilder;
import graphene.model.graphserver.GraphBuilderDirected;
import graphene.model.graphserver.GraphBuilderWithDirection;

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
