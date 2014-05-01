package graphene.enron.web.rest;

import graphene.dao.TransactionDAO;
import graphene.enron.model.graphserver.InteractionFinderEnronImpl;
import graphene.enron.model.sql.enron.EnronTransactionPair100;
import graphene.model.query.EventQuery;
import graphene.rest.ws.CSGraphServerRS;
import graphene.util.FastNumberUtils;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import mil.darpa.vande.InteractionFinder;
import mil.darpa.vande.TemporalGraphQuery;
import mil.darpa.vande.converters.cytoscapejs.V_CSGraph;
import mil.darpa.vande.generic.V_GraphQuery;
import mil.darpa.vande.interactions.InteractionGraphBuilder;
import mil.darpa.vande.legacy.GenericEdge;
import mil.darpa.vande.legacy.GenericGraph;
import mil.darpa.vande.legacy.GenericNode;
import mil.darpa.vande.legacy.cytoscapejs.CSGraph;
import mil.darpa.vande.legacy.graphml.GraphmlContainer;
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

public class CSGraphServerRSImpl implements CSGraphServerRS {

	@InjectService("Entity")
	private GraphBuilder entityGraphBuilder;
	@Inject
	private Logger logger;

	@InjectService("Transfer")
	private GraphBuilderWithDirection transferGraphBuilder;

	@Inject
	private TransactionDAO<EnronTransactionPair100, EventQuery> transferDAO;

	public CSGraphServerRSImpl() {

	}

	/**
	 * For Enron
	 * 
	 * @param email
	 * @return an approximation of the user name
	 */
	private String emailToName(String email) {
		int atpos = email.indexOf("@");
		if (atpos == -1)
			return null;
		String name = email.substring(0, atpos);

		int dotpos = name.indexOf(".");
		if (dotpos != -1) {
			try {
				String first = name.substring(0, dotpos);
				String last = name.substring(dotpos + 1);
				name = nameFix(first) + " " + nameFix(last);
				name = name.trim();
			} catch (Exception e) {
				System.out.println("Error fixing name " + name);
				return null;
			}
			;
		}
		return name;

	}

	private String fixup(String id) {
		id = id.replace("___", "/");
		id = id.replace("ZZZZZ", "#");
		return id;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see graphene.enron.ws.GraphServerRS#getByIdentifier(java.lang.String,
	 * java.lang.String, java.lang.String, java.lang.String, java.lang.String,
	 * boolean, boolean, boolean)
	 */
	@Override
	public CSGraph getByIdentifier(@PathParam("type") String type,
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
			return null;
		}

		// Customer/Dataset Specific hacks here
		for (GenericNode n : g.getNodes()) {

			String tp = n.getDataValue("IdentifierType");
			if ("customer".equals(tp)) {
				String c = n.getDataValue("Customer Number");
				if (c.charAt(0) == 'B') {
					n.addData("Borrower ID", c);
					n.setDataValue("IdentifierType", "BORROWER");
					n.setBackgroundColor("lightblue");
				} else {
					n.setDataValue("IdentifierType", "LENDER");
					n.addData("Lender ID", c);
				}

				n.removeData("Customer Number");
			}

			else if ("account".equals(tp))
				n.setDataValue("IdentifierType", "LOAN");
		}

		CSGraph m = new CSGraph(g, true);
		return m;

	}

	@Override
	public CSGraph getDirected(
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
			g = new GenericGraph();
		}

		// Fix ups for Enron-specific data
		for (GenericEdge e : g.getEdges()) {
			e.setWeight(e.getCount());
			e.setDataValue("IdentifierType", "DISTINCT EMAIL PAIR");
			e.setAmount(e.getCount()); // quick hack to get
										// the nbr of emails
										// shown on the edge
			// longer term we should have a data.label attribute that cytoscape
			// uses
		}
		for (GenericNode n : g.getNodes()) {
			n.setDataValue("IdentifierType", "EMAIL ADDRESS");
			String addr = n.getDataValue("entityName"); // actually the email
														// address
			// n.setDataValue("Identifier",addr); no - this causes loss of idVal
			// which we need to pivot
			n.setDataValue("Email", addr);
			n.setLabel(addr);
			n.removeData("entityName");
			String name = emailToName(addr);
			if (name == null) {
				logger.error("Could not get name for " + addr);
				name = addr;
			}
			if (name != null)
				n.addData("Name", name);

		}

		CSGraph m = new CSGraph(g, true);
		return m;

	}

	@Override
	public V_CSGraph getInteractionGraph(

			@PathParam("objectType") String objectType, // Dataset name etc
			@QueryParam("ids") String[] ids,
			@QueryParam("Type") String valueType,
			@QueryParam("maxHops") String maxHops,
			@QueryParam("maxNodes") String maxNodes,
			@QueryParam("maxEdgesPerNode") String maxEdgesPerNode,
			@QueryParam("showIcons") boolean showIcons,
			@QueryParam("fromdt") @DefaultValue(value = "0") String minSecs,
			@QueryParam("todt") @DefaultValue(value = "0") String maxSecs,
			@QueryParam("minLinksPairOverall") String minLinksPairOverall, /*
																			 * across
																			 * all
																			 * time
																			 * periods
																			 */
			@QueryParam("minValueAnyInteraction") String minValueAnyInteraction, /*
																				 * for
																				 * any
																				 * interaction
																				 */
			@QueryParam("daily") @DefaultValue(value = "false") boolean daily,
			@QueryParam("monthly") @DefaultValue(value = "false") boolean monthly,
			@QueryParam("yearly") @DefaultValue(value = "false") boolean yearly,
			@QueryParam("directed") @DefaultValue(value = "true") boolean directed) {
		logger.debug("-------");
		logger.debug("get Interaction Graph for type " + objectType);
		logger.debug("IDs     " + ids);
		logger.debug("Max Hops   " + maxHops);
		logger.debug("Max Nodes " + maxNodes);
		logger.debug("Max Edges " + maxEdgesPerNode);
		logger.debug("min links " + minLinksPairOverall);
		logger.debug("min value " + minValueAnyInteraction);
		logger.debug("daily " + daily);
		logger.debug("monthly " + monthly);
		logger.debug("yearly " + yearly);
		logger.debug("directed " + directed);

		// NB: min weight does not work. It is intended to say don't count edges
		// unless they occur X times (i.e. a called b + b called a > X)
		// However we are not iterating through the calls - we are using
		// SELECT DISTINCT for now.

		TemporalGraphQuery gq = new TemporalGraphQuery();

		gq.setMaxHops(FastNumberUtils.parseIntWithCheck(maxHops, 3));
		gq.setMaxNodes(FastNumberUtils.parseIntWithCheck(maxNodes, 500));
		gq.setMaxEdgesPerNode(FastNumberUtils.parseIntWithCheck(
				maxEdgesPerNode, 50));
		gq.setMinLinks(FastNumberUtils
				.parseIntWithCheck(minLinksPairOverall, 2));
		gq.setMinTransValue(FastNumberUtils.parseIntWithCheck(
				minValueAnyInteraction, 0));
		gq.setByMonth(monthly);
		gq.setByDay(daily);
		gq.setByYear(yearly);
		gq.setDirected(directed);

		Long endDate = null;
		try {
			endDate = Long.parseLong(maxSecs);
		} catch (Exception e) {
		}
		gq.setEndTime(endDate);

		Set<String> idSet = new HashSet<String>();
		String[] values;

		for (String v : ids)
			gq.addSearchId(v);

		logger.debug(gq.toString());

		InteractionFinder finder = new InteractionFinderEnronImpl(transferDAO);
		InteractionGraphBuilder b = new InteractionGraphBuilder(gq);
		mil.darpa.vande.generic.V_GenericGraph g = b.makeGraphResponse(gq,
				finder);

		logger.debug("Made graph with " + g.getNodes().size() + " Nodes and "
				+ g.getEdges().size() + " Edges");

		V_CSGraph m = new V_CSGraph(g, true);
		return m;
	}

	@Override
	public CSGraph getInteractions(String objectType, String value,
			String valueType, String degree, String maxNodes,
			String maxEdgesPerNode, boolean showIcons, String minSecs,
			String maxSecs, String minimumWeight) {
		logger.debug("-------");
		logger.debug("get Interaction Graph for type " + objectType);
		logger.debug("Value     " + value);
		logger.debug("Degrees   " + degree);
		logger.debug("Max Nodes " + maxNodes);
		logger.debug("Max Edges " + maxEdgesPerNode);
		logger.debug("min weight " + minimumWeight);

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

		GraphBuilder graphBuilder = transferGraphBuilder;

		V_GraphQuery q = new V_GraphQuery();
		q.setStartTime(startDate);
		q.setEndTime(endDate);
		for (String ac : values) {
			q.addSearchId(ac);
		}
		((GraphBuilderWithDirection) graphBuilder).setQuery(q); // so we know
																// any dates or
																// other
																// restrictions
		((GraphBuilderWithDirection) graphBuilder).setMinWeight(minWeight);

		// TODO: other types in due course
		graphBuilder.setStyle(GQT_Style);

		graphBuilder.setMaxNodes(maxnodes);
		graphBuilder.setMaxEdgesPerNode(maxedges);

		GraphmlContainer r = null;
		GenericGraph g = null;
		CSGraph m = null;
		try {
			g = graphBuilder
					.makeGraphResponse(valueType, values, maxdegree, "");
			m = new CSGraph(g, true);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return m;
	}

	private String nameFix(String name) {
		if (name == null || name.length() == 0)
			return name;
		else
			return ("" + name.charAt(0)).toUpperCase() + name.substring(1);
	}
}
