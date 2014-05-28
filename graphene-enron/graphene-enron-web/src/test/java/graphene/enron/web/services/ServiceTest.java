package graphene.enron.web.services;

import graphene.dao.EntityRefDAO;
import graphene.dao.TransactionDAO;
import graphene.dao.sql.DAOSQLModule;
import graphene.enron.dao.EnronDAOModule;
import graphene.enron.model.graphserver.GraphServerModule;
import graphene.enron.model.graphserver.PropertyGraphBuilderUnrolled;
import graphene.enron.model.sql.enron.EnronEntityref100;
import graphene.enron.model.sql.enron.EnronTransactionPair100;
import graphene.model.query.EntityRefQuery;
import graphene.model.query.EventQuery;
import graphene.util.ConnectionPoolModule;
import graphene.util.UtilModule;
import graphene.util.db.DBConnectionPoolService;
import mil.darpa.vande.generic.V_GenericEdge;
import mil.darpa.vande.generic.V_GenericGraph;
import mil.darpa.vande.generic.V_GenericNode;
import mil.darpa.vande.interactions.InteractionFinder;
import mil.darpa.vande.interactions.InteractionGraphBuilder;
import mil.darpa.vande.property.PropertyFinder;
import mil.darpa.vande.property.PropertyGraphBuilder;

import org.apache.tapestry5.ioc.Registry;
import org.apache.tapestry5.ioc.RegistryBuilder;
import org.slf4j.Logger;
import org.testng.annotations.BeforeSuite;

public class ServiceTest {

	protected Registry registry;
	protected DBConnectionPoolService cp;
	protected Logger logger;
	protected PropertyGraphBuilder pgb;
	protected InteractionGraphBuilder igb;
	protected EntityRefDAO<EnronEntityref100, EntityRefQuery> dao;
	protected TransactionDAO<EnronTransactionPair100, EventQuery> transactionDAO;
	protected InteractionFinder interactionFinder;
	protected PropertyFinder propertyFinder;
	protected PropertyGraphBuilderUnrolled pgbu;

	protected void printGraph(V_GenericGraph g) {
		System.out.println("=====================");
		for (V_GenericNode x : g.getNodes()) {
			System.out.println(x);
		}
		for (V_GenericEdge x : g.getEdges()) {
			System.out.println(x);
		}
		System.out.println("=====================");
	}

	@BeforeSuite
	public void setup() {

		RegistryBuilder builder = new RegistryBuilder();
		builder.add(UtilModule.class);
		builder.add(ConnectionPoolModule.class);
		builder.add(DAOSQLModule.class);
		builder.add(GraphServerModule.class);
		builder.add(EnronDAOModule.class);
		registry = builder.build();
		registry.performRegistryStartup();
		cp = registry.getService("GrapheneConnectionPool",
				DBConnectionPoolService.class);
		logger = registry.getService(Logger.class);

		assert registry != null;

		dao = registry.getService(EntityRefDAO.class);
		transactionDAO = registry.getService(TransactionDAO.class);

		pgb = registry.getService(PropertyGraphBuilder.class);
		pgbu = registry.getService(PropertyGraphBuilderUnrolled.class);
		propertyFinder = registry.getService(PropertyFinder.class);
		igb = registry.getService(InteractionGraphBuilder.class);
		interactionFinder = registry.getService(InteractionFinder.class);
		do {
			System.out.println("Waiting for EntityRefDAO to be available.");
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} while (!dao.isReady());
	}
}
