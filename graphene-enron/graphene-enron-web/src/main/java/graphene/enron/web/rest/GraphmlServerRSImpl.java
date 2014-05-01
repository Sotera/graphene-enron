package graphene.enron.web.rest;

import graphene.model.query.EventQuery;
import graphene.rest.ws.GraphmlServerRS;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import mil.darpa.vande.generic.V_GraphQuery;
import mil.darpa.vande.legacy.GenericGraph;
import mil.darpa.vande.legacy.graphml.GraphmlContainer;
import mil.darpa.vande.legacy.graphml.GraphmlGraph;
import mil.darpa.vande.legacy.graphserver.GraphBuilder;
import mil.darpa.vande.legacy.graphserver.GraphBuilderWithDirection;

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

public class GraphmlServerRSImpl implements GraphmlServerRS {

	@InjectService("Entity")
	private GraphBuilder entityGraphBuilder;
	@InjectService("Transfer")
	private GraphBuilderWithDirection transferGraphBuilder;

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

		int maxdegree = 6;
		try {
			maxdegree = Integer.parseInt(degree);
		} catch (Exception e) {
			maxdegree = 6;
		}

		int maxnodes = 1000; // MFM changed from 5000 to 1000
		try {
			maxnodes = Integer.parseInt(maxNodes);
		} catch (Exception e) {
			maxnodes = 1000; // MFM changed from 5000 to 1000
		}

		int maxedges = 50;
		try {
			maxedges = Integer.parseInt(maxEdgesPerNode);
		} catch (Exception e) {
			maxedges = 50;
		}

		boolean GQT_Style = !showIcons;

		entityGraphBuilder.setStyle(!showIcons);
		entityGraphBuilder.setStyle(GQT_Style);

		entityGraphBuilder.setMaxNodes(maxnodes);
		entityGraphBuilder.setMaxEdgesPerNode(maxedges);
		entityGraphBuilder.setBiPartite(bipartite);
		entityGraphBuilder.setShowNameNodes(showNameNodes);

		value = fixup(value);
		String[] values;

		if (type.contains("list")) {
			values = value.split("_");
		} else
			values = new String[] { value };

		GenericGraph g = null;
		try {
			g = entityGraphBuilder.makeGraphResponse(type, values, maxdegree,
					"");
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

		// NB: min weight does not work. It is intended to say don't count edges
		// unless they occur X times (i.e. a called b + b called a > X)
		// However we are not iterating through the calls - we are using
		// SELECT DISTINCT for now.

		int maxdegree = 6;
		try {
			maxdegree = Integer.parseInt(degree);
		} catch (Exception e) {
		}

		int maxnodes = 1000;
		try {
			maxnodes = Integer.parseInt(maxNodes);
		} catch (Exception e) {
		}
		int maxedges = 50;
		try {
			maxedges = Integer.parseInt(maxEdgesPerNode);
		} catch (Exception e) {
		}
		int minWeight = 0;
		try {
			minWeight = Integer.parseInt(minimumWeight);
		} catch (Exception e) {
		}
		;
		boolean GQT_Style = !showIcons;
		long startDate = 0;
		try {
			startDate = Long.parseLong(minSecs);
		} catch (Exception e) {
		}
		;
		long endDate = 0;
		try {
			endDate = Long.parseLong(maxSecs);
		} catch (Exception e) {
		}
		;

		String[] values;

		if (valueType.contains("list")) {
			values = value.split("_");
		} else
			values = new String[] { value };

		GraphBuilder graphBuilder = entityGraphBuilder;

		if (objectType.equalsIgnoreCase("transfers")) {
			V_GraphQuery q = new V_GraphQuery();
			q.setStartTime(startDate);
			q.setEndTime(endDate);
			for (String ac : values) {
				q.addSearchId(ac);
			}
			graphBuilder = transferGraphBuilder;
			((GraphBuilderWithDirection) graphBuilder).setQuery(q); // so we
																	// know any
																	// dates or
																	// other
																	// restrictions
			((GraphBuilderWithDirection) graphBuilder).setMinWeight(minWeight);
		}

		// TODO: other types in due course
		graphBuilder.setStyle(GQT_Style);

		graphBuilder.setMaxNodes(maxnodes);
		graphBuilder.setMaxEdgesPerNode(maxedges);

		GraphmlContainer r = null;
		GenericGraph g = null;
		try {
			g = graphBuilder
					.makeGraphResponse(valueType, values, maxdegree, "");
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
