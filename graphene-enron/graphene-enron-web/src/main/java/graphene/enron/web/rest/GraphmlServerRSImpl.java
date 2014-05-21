package graphene.enron.web.rest;

import graphene.rest.ws.GraphmlServerRS;
import graphene.util.FastNumberUtils;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import mil.darpa.vande.converters.graphml.GraphmlContainer;
import mil.darpa.vande.converters.graphml.GraphmlGraph;
import mil.darpa.vande.generic.V_GenericGraph;
import mil.darpa.vande.generic.V_GraphQuery;
import mil.darpa.vande.interactions.InteractionFinder;
import mil.darpa.vande.interactions.InteractionGraphBuilder;
import mil.darpa.vande.property.PropertyFinder;
import mil.darpa.vande.property.PropertyGraphBuilder;

import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.InjectService;
import org.slf4j.Logger;

/**
 * /** The REST Service to return a graph in SnaglML format which is an
 * extension of graphml.
 * 
 * @author PWG for DARPA
 * 
 */
@Deprecated
public class GraphmlServerRSImpl implements GraphmlServerRS {

	@InjectService("Entity")
	private PropertyGraphBuilder entityGraphBuilder;

	@InjectService("finder")
	private PropertyFinder propertyFinder;

	@InjectService("finder")
	private InteractionFinder interactionFinder;

	@InjectService("Transfer")
	private InteractionGraphBuilder interactionGraphBuilder;
	// private TransactionDistinctPairDAO finder;

	@Inject
	private Logger logger;

	public GraphmlServerRSImpl() {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see graphene.enron.ws.GraphServerRS#getByIdentifier(java.lang.String,
	 * java.lang.String, java.lang.String, java.lang.String, java.lang.String,
	 * boolean, boolean, boolean)
	 */
	@Override
	public GraphmlContainer getByIdentifier(@PathParam("type") String type,
			@PathParam("value") String value,
			@QueryParam("degree") String degree,
			@QueryParam("maxNodes") String maxNodes,
			@QueryParam("maxEdgesPerNode") String maxEdgesPerNode,
			@QueryParam("bipartite") boolean bipartite,
			@QueryParam("showLeafNodes") boolean leafNodes,
			@QueryParam("showNameNodes") boolean showNameNodes,
			@QueryParam("showIcons") boolean showIcons) {
		logger.trace("-------");
		logger.trace("getGraph for type " + type);
		logger.trace("Value     " + value);
		logger.trace("Degrees   " + degree);
		logger.trace("LeafNodes " + leafNodes);
		logger.trace("Max Nodes " + maxNodes);
		logger.trace("Max Edges " + maxEdgesPerNode);
		logger.trace("LeafNodes " + leafNodes);
		logger.trace("Bipartite " + bipartite);
		logger.trace("showNameNodes " + showNameNodes);

		int maxdegree = FastNumberUtils.parseIntWithCheck(degree, 6);
		int maxnodes = FastNumberUtils.parseIntWithCheck(maxNodes, 1000);
		int maxedges = FastNumberUtils.parseIntWithCheck(maxEdgesPerNode, 50);

		value = fixup(value);
		String[] values;

		if (type.contains("list")) {
			values = value.split("_");
		} else {
			values = new String[] { value };
		}

		V_GraphQuery q = new V_GraphQuery();

		q.addSearchIds(values);

		q.setMaxEdgesPerNode(maxedges);
		q.setMaxHops(maxdegree);
		q.setMaxNodes(maxnodes);
		q.setType(type); // new, --djue

		V_GenericGraph g = null;
		try {
			g = entityGraphBuilder.makeGraphResponse(q, propertyFinder);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		GraphmlGraph m = new GraphmlGraph(g, true);
		GraphmlContainer c = new GraphmlContainer();
		c.setGraph(m);
		return c;

	}

	@Override
	public GraphmlContainer getDirected(
			@PathParam("objectType") String objectType, // Dataset name etc
			@PathParam("value") String value,
			@QueryParam("Type") String valueType,
			@QueryParam("degree") String degree,
			@QueryParam("maxNodes") String maxNodes,
			@QueryParam("maxEdgesPerNode") String maxEdgesPerNode,
			@QueryParam("showIcons") boolean showIcons,
			@QueryParam("fromdt") @DefaultValue(value = "0") String minSecs,
			@QueryParam("todt") @DefaultValue(value = "0") String maxSecs,
			@QueryParam("minWeight") String minimumWeight) {
		logger.trace("-------");
		logger.trace("getGraph for type " + objectType);
		logger.trace("Value     " + value);
		logger.trace("Degrees   " + degree);
		logger.trace("Max Nodes " + maxNodes);
		logger.trace("Max Edges " + maxEdgesPerNode);
		logger.trace("min weight " + minimumWeight);

		// FIXME: Min weight is not working
		int maxdegree = FastNumberUtils.parseIntWithCheck(degree, 6);
		int maxnodes = FastNumberUtils.parseIntWithCheck(maxNodes, 1000);
		int maxedges = FastNumberUtils.parseIntWithCheck(maxEdgesPerNode, 50);
		int minWeight = FastNumberUtils.parseIntWithCheck(minimumWeight, 0);
		long startDate = FastNumberUtils.parseLongWithCheck(minSecs, 0);
		long endDate = FastNumberUtils.parseLongWithCheck(maxSecs, 0);

		String[] values;

		if (valueType.contains("list")) {
			values = value.split("_");
		} else
			values = new String[] { value };

		V_GraphQuery q = new V_GraphQuery();

		q.addSearchIds(values);

		q.setMaxEdgesPerNode(maxedges);
		q.setMaxHops(maxdegree);
		q.setMaxNodes(maxnodes);
		q.setStartTime(startDate);
		q.setEndTime(endDate);
		q.setType(valueType); // new, --djue
		q.setMinTransValue(minWeight); // new --djue

		InteractionGraphBuilder graphBuilder = null;
		if (objectType.equalsIgnoreCase("transfers")) {
			graphBuilder = interactionGraphBuilder;
		}
		V_GenericGraph g = null;
		try {
			g = graphBuilder.makeGraphResponse(q, interactionFinder);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		GraphmlGraph m = new GraphmlGraph(g, true);
		GraphmlContainer c = new GraphmlContainer();
		c.setGraph(m);
		return c;

	}

	private String fixup(String id) {
		id = id.replace("___", "/");
		id = id.replace("ZZZZZ", "#");
		return id;
	}

}
