package graphene.enron.web.rest;

import graphene.dao.TransactionDAO;
import graphene.enron.model.graphserver.InteractionFinderEnronImpl;
import graphene.enron.model.sql.enron.EnronTransactionPair100;
import graphene.model.query.EventQuery;
import graphene.rest.ws.CSGraphServerRS;
import graphene.util.FastNumberUtils;

import java.util.HashSet;
import java.util.Set;

import mil.darpa.vande.converters.cytoscapejs.V_CSGraph;
import mil.darpa.vande.generic.V_GenericGraph;
import mil.darpa.vande.generic.V_GenericNode;
import mil.darpa.vande.generic.V_GraphQuery;
import mil.darpa.vande.interactions.InteractionFinder;
import mil.darpa.vande.interactions.InteractionGraphBuilder;
import mil.darpa.vande.interactions.TemporalGraphQuery;
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

public class CSGraphServerRSImpl implements CSGraphServerRS {

	@InjectService("Entity")
	private PropertyGraphBuilder entityGraphBuilder;

	@InjectService("finder")
	private PropertyFinder propertyFinder;
	
	@InjectService("finder")
	private InteractionFinder interactionFinder;
	
	@InjectService("Transfer")
	private InteractionGraphBuilder interactionGraphBuilder;

	@Inject
	private Logger logger;

	@Inject
	private TransactionDAO<EnronTransactionPair100, EventQuery> transferDAO;

	public CSGraphServerRSImpl() {

	}

	/**
	 * For Enron
	 * 
	 * TODO: Move this to HTS module, or make it part of the ETL process --djue
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

		}
		return name;
	}

	/**
	 * WTF is this? --djue
	 * 
	 * @param id
	 * @return
	 */
	@Deprecated
	private String fixup(String id) {
		id = id.replace("___", "/");
		id = id.replace("ZZZZZ", "#");
		return id;
	}

	@Override
	public V_CSGraph getByIdentifier( String type,
		 String value,String degree,
			 String maxNodes,
			 String maxEdgesPerNode,
		 boolean bipartite,
		 boolean leafNodes,
		 boolean showNameNodes,
			 boolean showIcons) {
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

		V_GraphQuery q = new V_GraphQuery();

		q.setType(type); // new, --djue
		q.setMaxNodes(maxnodes);
		q.setMaxEdgesPerNode(maxedges);
		q.setMaxHops(maxdegree);
		value = fixup(value);
		String[] values;

		if (type.contains("list")) {
			values = value.split("_");
		} else {
			values = new String[] { value };
		}
			q.addSearchIds(values);
		
		V_GenericGraph g = null;
		try {
			g = entityGraphBuilder.makeGraphResponse(q, propertyFinder);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		// Customer/Dataset Specific hacks here
		for (V_GenericNode n : g.getNodes()) {

			String tp = n.getDataValue("IdentifierType");
			if ("customer".equals(tp)) {
				String c = n.getDataValue("Customer Number");
				if (c.charAt(0) == 'B') {
					n.addData("Borrower ID", c);
					n.setDataValue("IdentifierType", "BORROWER");
					// n.setBackgroundColor("lightblue");
				} else {
					n.setDataValue("IdentifierType", "LENDER");
					n.addData("Lender ID", c);
				}

				n.removeData("Customer Number");
			}

			else if ("account".equals(tp)) {
				n.setDataValue("IdentifierType", "LOAN");
			}
		}

		V_CSGraph m = new V_CSGraph(g, true);
		logger.debug(m.toString());
		return m;

	}

	// @Override
	// public V_CSGraph getDirected(
	// @PathParam("objectType") String objectType, // Dataset name etc
	// @PathParam("value") String value,
	// @QueryParam("Type") String valueType,
	// @QueryParam("degree") String degree,
	// @QueryParam("maxNodes") String maxNodes,
	// @QueryParam("maxEdgesPerNode") String maxEdgesPerNode,
	// @QueryParam("showIcons") boolean showIcons,
	// @QueryParam("fromdt") @DefaultValue(value = "0") String minSecs,
	// @QueryParam("todt") @DefaultValue(value = "0") String maxSecs,
	// @QueryParam("minWeight") String minimumWeight) {
	// logger.trace("-------");
	// logger.trace("getGraph for type " + objectType);
	// logger.trace("Value     " + value);
	// logger.trace("Degrees   " + degree);
	// logger.trace("Max Nodes " + maxNodes);
	// logger.trace("Max Edges " + maxEdgesPerNode);
	// logger.trace("min weight " + minimumWeight);
	//
	// // NB: min weight does not work. It is intended to say don't count edges
	// // unless they occur X times (i.e. a called b + b called a > X)
	// // However we are not iterating through the calls - we are using
	// // SELECT DISTINCT for now.
	//
	// int maxdegree = FastNumberUtils.parseIntWithCheck(degree, 6);
	// int maxnodes = FastNumberUtils.parseIntWithCheck(maxNodes, 1000);
	// int maxedges = FastNumberUtils.parseIntWithCheck(maxEdgesPerNode, 50);
	// int minWeight = FastNumberUtils.parseIntWithCheck(minimumWeight, 0);
	// long startDate = FastNumberUtils.parseLongWithCheck(minSecs, 0);
	// long endDate = FastNumberUtils.parseLongWithCheck(maxSecs, 0);
	//
	// String[] values;
	//
	// if (valueType.contains("list")) {
	// values = value.split("_");
	// } else {
	// values = new String[] { value };
	// }
	// V_GraphQuery q = new V_GraphQuery();
	// q.setStartTime(startDate);
	// q.setEndTime(endDate);
	// q.setType(valueType); // new, --djue
	// q.setMinTransValue(minWeight); // new --djue
	// q.setMaxNodes(maxnodes);
	// q.setMaxEdgesPerNode(maxedges);
	// for (String ac : values) {
	// q.addSearchId(ac);
	// }
	// GraphBuilder graphBuilder = entityGraphBuilder;
	//
	// if (objectType.equalsIgnoreCase("transfers")) {
	//
	// graphBuilder = interactionGraphBuilder;
	// // We now send the dao and query to the graph builder when we do the
	// // actual call --djue
	// // ((GraphBuilderWithDirection) graphBuilder).setQuery(q); // so we
	// // // know any
	// // // dates or
	// // // other
	// // // restrictions
	// // ((GraphBuilderWithDirection)
	// // graphBuilder).setMinWeight(minWeight);
	// }
	//
	// V_GenericGraph g = new V_GenericGraph();
	// try {
	// g = graphBuilder.makeGraphResponse(q, finder);
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	//
	// // Fix ups for Enron-specific data
	// for (V_GenericEdge e : g.getEdges()) {
	// e.setWeight(e.getCount());
	// e.setDataValue("IdentifierType", "DISTINCT EMAIL PAIR");
	// e.setCount(e.getCount()); // quick hack to get the size of the enron
	// // email
	// e.setWeight(e.getCount()); // quick hack to get the size of the
	// // enron email
	// // TODO: longer term we should have a data.label attribute that
	// // cytoscape
	// // uses
	// }
	// for (V_GenericNode n : g.getNodes()) {
	// n.setDataValue("IdentifierType", "EMAIL ADDRESS");
	// String addr = n.getDataValue("entityName"); // actually the email
	// // address
	// // n.setDataValue("Identifier",addr); no - this causes loss of idVal
	// // which we need to pivot
	// n.setDataValue("Email", addr);
	// n.setLabel(addr);
	// n.removeData("entityName");
	// String name = emailToName(addr);
	// if (name == null) {
	// logger.error("Could not get name for " + addr);
	// name = addr;
	// }
	// if (name != null) {
	// n.addData("Name", name);
	// }
	// }
	//
	// V_CSGraph m = new V_CSGraph(g, true);
	// logger.debug(m.toString());
	// return m;
	//
	// }

	@Override
	public V_CSGraph getInteractionGraph(
			String objectType, 
			String[] ids,
			String valueType,
			 String maxHops,
			 String maxNodes,
			 String maxEdgesPerNode,
			 boolean showIcons,
			String minSecs,
			 String maxSecs,
			 String minLinksPairOverall, 
		String minValueAnyInteraction, 
		boolean daily,
			 boolean monthly,
	 boolean yearly,
			 boolean directed) {
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

		long startDate = FastNumberUtils.parseLongWithCheck(minSecs, 0);
		long endDate = FastNumberUtils.parseLongWithCheck(maxSecs, 0);

		gq.setEndTime(endDate);

		Set<String> idSet = new HashSet<String>();
		String[] values;
		// FIXME: I think you meant to split the ids value into multiple ones.

			gq.addSearchIds(ids);
		
		logger.debug(gq.toString());

		InteractionFinder finder = new InteractionFinderEnronImpl(transferDAO);
		InteractionGraphBuilder b = new InteractionGraphBuilder();
		b.setOriginalQuery(gq);
		mil.darpa.vande.generic.V_GenericGraph g = b.makeGraphResponse(gq,
				finder);

		logger.debug("Made graph with " + g.getNodes().size() + " Nodes and "
				+ g.getEdges().size() + " Edges");

		V_CSGraph m = new V_CSGraph(g, true);
		logger.debug(m.toString());
		return m;
	}

	@Override
	public V_CSGraph getInteractions(final String objectType,
			final String value, final String valueType, final String degree,
			final String maxNodes, final String maxEdgesPerNode,
			final boolean showIcons, final String minSecs,
			final String maxSecs, final String minimumWeight) {
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

		int maxdegree = FastNumberUtils.parseIntWithCheck(degree, 6);
		int maxnodes = FastNumberUtils.parseIntWithCheck(maxNodes, 1000);
		int maxedges = FastNumberUtils.parseIntWithCheck(maxEdgesPerNode, 50);
		int minWeight = FastNumberUtils.parseIntWithCheck(minimumWeight, 0);
		long startDate = FastNumberUtils.parseLongWithCheck(minSecs, 0);
		long endDate = FastNumberUtils.parseLongWithCheck(maxSecs, 0);
		String[] values;

		if (valueType.contains("list")) {
			values = value.split("_");
		} else {
			values = new String[] { value };
		}

		V_GraphQuery q = new V_GraphQuery();
		q.setMaxHops(maxdegree);
		q.setStartTime(startDate);
		q.setEndTime(endDate);
		q.setType(valueType); // new, --djue
		q.setMinTransValue(minWeight); // new --djue

			q.addSearchIds(values);
		
		q.setMaxNodes(maxnodes);
		q.setMaxEdgesPerNode(maxedges);

		V_GenericGraph g = null;
		V_CSGraph m = null;
		try {
			g = interactionGraphBuilder.makeGraphResponse(q, interactionFinder);
			// g = graphBuilder
			// .makeGraphResponse(valueType, values, maxdegree, "");
			m = new V_CSGraph(g, true);
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.debug(m.toString());
		return m;
	}

	private String nameFix(String name) {
		if (name == null || name.length() == 0) {
			return name;
		} else {
			return ("" + name.charAt(0)).toUpperCase() + name.substring(1);
		}
	}
}
