package graphene.enron.model.graphserver;

import graphene.dao.TransactionDistinctPairDAO;
import graphene.model.graph.GenericGraph;
import graphene.model.graphserver.GraphBuilder;
import graphene.model.graphserver.GraphBuilderDirected;

import org.apache.tapestry5.ioc.annotations.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author PWG for DARPA
 * 
 */


public class GraphBuilderTransferImpl extends GraphBuilderDirected implements
		GraphBuilder {

	@Inject
	private TransactionDistinctPairDAO<?,?> dao;

	static Logger logger = LoggerFactory.getLogger(GraphBuilderTransferImpl.class);

	@Override
	public GenericGraph makeGraphResponse(String type, String[] values,
			int maxDegree, String nodeType) {

		if (dao == null) {
			logger.error("Null dao");
			return null;
		}
		setLoader(dao);
		try {
			return super.makeGraphResponse(type, values, maxDegree, "account");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

}
