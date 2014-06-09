package graphene.enron.ingest;

import graphene.dao.neo4j.DAONeo4JEModule;
import graphene.util.UtilModule;

import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.annotations.SubModule;

@SubModule({ UtilModule.class, DAONeo4JEModule.class })
public class IngestModule {
	public static void bind(ServiceBinder binder) {

	}




}
