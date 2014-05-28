package graphene.enron.model.graphserver;

import graphene.dao.EntityRefDAO;
import graphene.dao.IdTypeDAO;
import graphene.enron.model.sql.enron.EnronEntityref100;
import graphene.enron.model.sql.enron.EnronIdentifierType100;
import graphene.model.idl.G_CanonicalPropertyType;
import graphene.model.idl.G_RelationshipType;
import graphene.model.idl.G_SearchType;
import graphene.model.query.EntityRefQuery;
import graphene.model.query.EntitySearchTuple;
import graphene.model.query.StringQuery;
import graphene.util.G_CallBack;
import graphene.util.validator.ValidationUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mil.darpa.vande.generic.V_EdgeList;
import mil.darpa.vande.generic.V_GenericEdge;
import mil.darpa.vande.generic.V_GenericGraph;
import mil.darpa.vande.generic.V_GenericNode;
import mil.darpa.vande.generic.V_GraphQuery;
import mil.darpa.vande.generic.V_NodeList;

import org.apache.tapestry5.ioc.annotations.Inject;
import org.slf4j.Logger;

public class PropertyGraphBuilderUnrolled implements
		G_CallBack<EnronEntityref100> {

	public static final char ENTITY_ACCOUNT = 'A';

	public static final char ENTITY_CUSTOMER = 'C';

	public static final char ENTITY_IDENTIFIER = 'I';

	private V_EdgeList edgeList;
	private Map<String, V_GenericEdge> edgeMap;
	private IdTypeDAO<EnronIdentifierType100, StringQuery> idTypeDAO;

	@Inject
	private Logger logger;

	private V_NodeList nodeList;
	private EntityRefDAO<EnronEntityref100, EntityRefQuery> propertyDAO;

	public PropertyGraphBuilderUnrolled(IdTypeDAO idTypeDAO,
			EntityRefDAO propertyDAO) {
		this.idTypeDAO = idTypeDAO;
		this.propertyDAO = propertyDAO;

	}

	@Override
	public boolean callBack(EnronEntityref100 p) {

		// XXX:What we really want is just a list of Actors, which is like an
		// interaction (except an Interaction has only two actors)
		// But we'll use a node list in a pinch. This is the one we'll return.
		String custno = p.getCustomernumber();
		String acno = p.getAccountnumber();
		String idval = p.getIdentifier();// .getIdValue();

		// String s_acno = p.getSenderId().toString();
		// String s_acname = p.getSenderValueStr();
		//
		// String t_acno = p.getReceiverId().toString();
		// String t_acname = p.getReceiverValueStr();

		V_GenericNode custNode = null, acnoNode = null, idNode = null;
		if (ValidationUtils.isValid(custno)) {
			custNode = new V_GenericNode(custno);

			// V_Actor a = new V_Actor(custno);
			custNode.setIdType(ENTITY_CUSTOMER);
			custNode.setIdVal(custno);
			custNode.setLabel(custno);
			// value type is "customer"

			/*
			 * This is kind of business logic-like. The customer node also gets
			 * any id properties baked in.
			 */
			if (ValidationUtils.isValid(idval)) {
				int idTypeId = p.getIdtypeId();
				custNode.addProperty("ShortName",
						idTypeDAO.getShortName(idTypeId));
				custNode.addProperty("Value", idval);
				custNode.addProperty("Family", idTypeDAO.getFamily(idTypeId));
				// .addData(shortName, idval, family);
			}
			nodeList.addNode(custNode);
		}
		if (ValidationUtils.isValid(acno)) {
			acnoNode = new V_GenericNode(acno);
			acnoNode.setIdType(ENTITY_ACCOUNT);
			acnoNode.setIdVal(acno);
			acnoNode.setLabel(acno);
			nodeList.addNode(acnoNode);
		}

		if (ValidationUtils.isValid(idval)) {
			idNode = new V_GenericNode(idval);
			idNode.setIdType(ENTITY_IDENTIFIER);
			idNode.setIdVal(idval);
			idNode.setLabel(idval);
			nodeList.addNode(idNode);
		}
		if (custNode != null && idNode != null) {
			V_GenericEdge v = new V_GenericEdge(custNode, idNode);
			v.setIdType(G_RelationshipType.RELATED_TO_ID.name());
			// edgeList.addEdge(v);
			edgeMap.put(generateKey(v), v);
		}

		if (custNode != null && acnoNode != null) {
			V_GenericEdge v = new V_GenericEdge(custNode, acnoNode);
			v.setIdType(G_RelationshipType.RELATED_TO_ID.name());
			// edgeList.addEdge(v);
			edgeMap.put(generateKey(v), v);
		}

		logger.debug("++++++++++++++++++++++++++++++++++++++");
		logger.debug("At the end of the callback, nodeList is now: " + nodeList);
		logger.debug("++++++++++++++++++++++++++++++++++++++");
		logger.debug("At the end of the callback, edgeMap is now: " + edgeMap);
		logger.debug("++++++++++++++++++++++++++++++++++++++");
		return true;
	}

	/**
	 * Doesn't matter what this does, as long as it is unique.
	 * 
	 * @param v
	 * @return
	 */
	private String generateKey(V_GenericEdge v) {
		return "s:" + v.getSourceId() + "-->t:" + v.getTargetId();
	}

	public V_GenericGraph makeGraphResponse(final V_GraphQuery q)
			throws Exception {
		if (q.getMaxHops() <= 0) {
			return new V_GenericGraph();
		}

		this.nodeList = new V_NodeList();
		this.edgeList = new V_EdgeList(q);
		this.edgeMap = new HashMap<String, V_GenericEdge>();
		int intStatus = 0;
		String strStatus = "Graph Loaded";

		char nodeType;
		Set<String> scannedActors = new HashSet<String>();
		List<String> idList = new ArrayList<String>();
		idList.addAll(q.getSearchIds());

		// First hop, based on the original query. Will call us back for each
		// row

		// FIXME: This call uses side effects.
		EntityRefQuery eq = new EntityRefQuery();

		// add to attribute list
		for (String id : idList) {
			eq.getAttributeList().add(
					new EntitySearchTuple<String>(G_SearchType.COMPARE_EQUALS,
							G_CanonicalPropertyType.ANY, id));
		}
		propertyDAO.performCallback(0, 0, this, eq);
		// finder.query(idList, q, this);

		scannedActors.addAll(idList);

		// Mark the original query nodes as "origin" so they can be shown
		// in a different color

		for (V_GenericNode n : nodeList.getNodes()) {
			for (String s : idList) {
				if (s.equals(n.getId())) {
					n.setOrigin(true);
				}
			}
		}

		// Do the hops. Note that we don't know whether we have exceeded the max
		// nodes
		// until after each hop. So each time we don't exceed this number we
		// save
		// the state. Then if we exceed maxNodes we can return the last version
		// that
		// didn't exceed it.

		V_NodeList savNodeList = nodeList.clone();
		// V_EdgeList savEdgeList = edgeList.clone();
		Map saveEdgeMap = new HashMap(edgeMap);

		// aka traversals from legacy--djue
		// FIXME: No reason to use prefix increment here
		int hop = 1;
		for (hop = 1; hop <= q.getMaxHops()
				&& nodeList.getNodes().size() < q.getMaxNodes(); hop++) {
			// this was called traverse() in the legacy code. --djue
			oneHop(scannedActors, q);
			savNodeList = nodeList.clone();
			// savEdgeList = edgeList.clone();
			saveEdgeMap = new HashMap(edgeMap);
		}
		if (nodeList.getNodes().size() > q.getMaxNodes()) {
			nodeList = savNodeList;
			// edgeList = savEdgeList;
			edgeMap = saveEdgeMap;
			intStatus = 1; // will trigger the message.
			strStatus = "Returning only " + hop
					+ " hops, as maximum nodes you requested would be exceeded";
		}

		
		//NOW finally add in all those unique edges.
		for (V_GenericEdge e : edgeMap.values()) {
			edgeList.addEdge(e);
		}
		nodeList.removeOrphans(edgeList);
		V_GenericGraph g = new V_GenericGraph(nodeList.getNodes(),
				edgeList.getEdges());
		g.setIntStatus(intStatus);
		g.setStrStatus(strStatus);
		return g;
	}

	private void oneHop(final Set<String> scannedActors, final V_GraphQuery q) {
		List<String> idList = new ArrayList<String>();
		// Iterate over each node
		for (V_GenericNode n : nodeList.getNodes()) {
			String id = n.getId();
			// if we haven't scanned
			if (!scannedActors.contains(id)) {
				// Make sure there aren't too many edges.
				
				/**
				 * It seems to take longer to count the edges than to just return them from the in memory implementation.
				 * 
				 */
				long count = 0;
				try {
					count = propertyDAO.countEdges(id);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				n.setNbrLinks((int) count);
				if (count > q.getMaxEdgesPerNode()) {
					n.setCluster(true);
				} else {
					idList.add(id);
				}
				scannedActors.add(id);
			}
		}
		if (idList.size() > 0) {
			EntityRefQuery eq = new EntityRefQuery();
			// add to attribute list
			for (String id : idList) {
				eq.getAttributeList().add(
						new EntitySearchTuple<String>(
								G_SearchType.COMPARE_EQUALS,
								G_CanonicalPropertyType.ANY, id));
			}
			propertyDAO.performCallback(0, 0, this, eq);
			// finder.query(idList, q, this);
		}
	}

}
