package graphene.enron.model.graphserver;

import graphene.dao.EntityRefDAO;
import graphene.dao.IdTypeDAO;
import graphene.enron.model.sql.enron.EnronEntityref100;
import graphene.enron.model.sql.enron.EnronIdentifierType100;
import graphene.model.graph.GenericEdge;
import graphene.model.graph.GenericGraph;
import graphene.model.graph.GenericNode;
import graphene.model.graph.entity.EntityRefNode;
import graphene.model.graph.entity.NodeFactory;
import graphene.model.graph.entity.NodeList;
import graphene.model.graph.graphml.GraphmlContainer;
import graphene.model.graphserver.GraphBuilder;
import graphene.model.idl.G_CanonicalPropertyType;
import graphene.model.idl.G_SearchType;
import graphene.model.query.EntitySearchTuple;
import graphene.model.query.EntityRefQuery;
import graphene.model.view.entities.IdType;
import graphene.util.stats.TimeReporter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.tapestry5.ioc.annotations.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a version of graphbuilder that uses EntityRef to graph entities 
 * and their shared attributes
 * 
 * @author PWG
 * 
 */
public class GraphBuilderEntityImpl implements GraphBuilder {
	static Logger logger = LoggerFactory
			.getLogger(GraphBuilderEntityImpl.class);

	private int maxNodes = 300; // MFM
	private int maxEdgesPerNode = 50; //

	private NodeFactory nodeFactory = new NodeFactory();

	private NodeList nodeList = new NodeList();
	@Inject
	private EntityRefDAO<EnronEntityref100, EntityRefQuery> dao;

	@Inject
	private IdTypeDAO<EnronIdentifierType100, String> idTypeDAO;

	// Set up some local structures so we don't have to keep passing them around
	private Map<String, GenericEdge> edgeMap = new HashMap<String, GenericEdge>();
	// private boolean customerCentric = true;
	// private boolean accountCentric = false;

	private boolean GQT_Style = false;

	private boolean biPartite;

	private boolean showLeafNodes;

	private boolean showNameNodes;

	// Note that accounts and identifiers and customers already
	// have lists of associated items, generated at load time.

	/**
	 * Constructor.
	 * 
	 * @param GQT_Style
	 *            boolean. True means show the nodes as text so as to resemble
	 *            the legacy Graph Query Tool
	 * @param i
	 */
	public GraphBuilderEntityImpl() {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * graphene.enron.services.graphserver.GraphB#makeGraphResponse(java.lang.
	 * String, java.lang.String[], int)
	 */
	@Override
	public GenericGraph makeGraphResponse(String type, String[] values,
			int maxDegree, String unusedStaticNodeType) throws Exception {

		nodeFactory = new NodeFactory();
		nodeList = new NodeList();
		edgeMap = new HashMap<String, GenericEdge>();

		char nodeType;

		if (maxDegree <= 0) // guard against infinite recursion
			return new GenericGraph();
		TimeReporter t = new TimeReporter("Calculate Graph", logger);

		nodeFactory.reset();

		Set<EnronEntityref100> rowList = new HashSet<EnronEntityref100>();

		EntityRefQuery q;

		q = new EntityRefQuery();
                q.setMaxResult(300000);  // MFM added

		if (type.startsWith("customer")) {
			for (String v : values) {
				q.getAttributeList().add(
						new EntitySearchTuple<String>(
								G_SearchType.COMPARE_EQUALS,
								G_CanonicalPropertyType.CUSTOMER_NUMBER, v));
			}
			nodeType = EntityRefNode.ENTITY_CUSTOMER;
		}

		else if (type.startsWith("account")) {
			for (String v : values) {
				q.getAttributeList().add(
						new EntitySearchTuple<String>(
								G_SearchType.COMPARE_EQUALS,
								G_CanonicalPropertyType.ACCOUNT, v));
			}
			nodeType = EntityRefNode.ENTITY_ACCOUNT;
		}

		else {
			for (String v : values) {
				q.getAttributeList().add(
						new EntitySearchTuple<String>(
								G_SearchType.COMPARE_EQUALS,
								G_CanonicalPropertyType.ANY, v));
			}
			nodeType = EntityRefNode.ENTITY_IDENTIFIER;
		}
		rowList.addAll(dao.rowSearch(q));

		makeNodes(rowList, 1);
		for (EntityRefNode n : getNodesByType(nodeType)) {
			n.setOrigin(true);
		}

		// Traversals

		int degree = 2;
		int count = 0;
		for (degree = 2; degree < maxDegree; ++degree) {
			rowList = traverse(degree);
			count = makeNodes(rowList, degree);
			logger.debug("After traverse for degree " + degree
					+ " Node count is " + count);
			if (count > maxNodes) {
				logger.debug("Too many nodes: will truncate");
				// truncate(degree);
				break;
			}
		}

		// temp fix for too many nodes, until truncate is working
		if (count > maxNodes) {
			logger.debug("About to recurse");
			return makeGraphResponse(type, values, maxDegree - 1, ""); // recurse
		}
		// end temp fix

		rowList = finishCustomers(degree);
		makeNodes(rowList, degree); // for the finish customers

		// We have either reached the maxdegrees or maxnodes
		// We have a node for every identifier, account and customer that was
		// scanned
		// We have edges between every customer node and every account node
		// We have edges beteen every customer node and every identifier node

		// We don't want to show identifier nodes where either (a) there are
		// more than
		// maxNodes edges (placeholder) or (b) there is only one edge.

		removeLeafNodes();
		removeOrphanEdges();

		t.logAsCompleted();
		List<GenericNode> nodes = new ArrayList<GenericNode>();
		nodes.addAll(nodeList.getAllNodes());
		
		for (GenericNode n:nodes) {
			EntityRefNode en = (EntityRefNode) n;
			en.setColors(true);
		}
		
		
		List<GenericEdge> edges = new ArrayList<GenericEdge>();
		edges.addAll(edgeMap.values());
		return new GenericGraph(nodes,edges);
	}

	/**
	 * Make nodes and edges for all items in the set of rows from the data table
	 * if not already present
	 * 
	 * @param rowList
	 *            
	 * @param degree
	 *            int current degree of the graph
	 * @return int the number of nodes currently in the graph
	 */
	private int makeNodes(Set<EnronEntityref100> rowList, int degree) {
		EntityRefNode idNode = null, custNode, acNode = null;

		logger.debug("Scanning " + rowList.size() + " DB Rows");

		for (EnronEntityref100 row : rowList) {

			String custno = row.getCustomernumber();
			String acno = row.getAccountnumber();
			String idval = row.getIdentifier();// .getIdValue();

			custNode = idNode = acNode = null;

			if (custno.length() > 0) {
				custNode = (EntityRefNode) nodeList.getNode(
						EntityRefNode.ENTITY_CUSTOMER, custno);
				if (custNode == null) {
					custNode = nodeFactory.makeCustomerNode(custno, degree);
					nodeList.addNode(custNode);
				}
			}

			// Make the account node if not there

			if (acno.length() > 0) {
				acNode = (EntityRefNode) nodeList.getNode(
						EntityRefNode.ENTITY_ACCOUNT, acno);
				if (acNode == null) {
					acNode = nodeFactory.makeAccountNode(acno, degree);
					acNode.addData("entityId", custno);
					nodeList.addNode(acNode);
				}
			}

			// Make the identifier node if not there and otherwise valid

			if (idval.length() > 0) {
				idNode = (EntityRefNode) nodeList.getNode(
						EntityRefNode.ENTITY_IDENTIFIER, idval);
				if (idNode == null) { // First time for this ID
					idNode = nodeFactory.makeIdentifierNode(idval, degree);
					IdType idType = idTypeDAO.getByType(row.getIdtypeId());
					if (idType != null) {
						idNode.setId_type(idType);
						idNode.setValueType(idType.getFamily());
					}
					nodeList.addNode(idNode);
				}
				if (custNode != null) {
					String shortName = idTypeDAO
							.getShortName(row.getIdtypeId());
					String family = idTypeDAO.getFamily(row.getIdtypeId());
					custNode.addData(shortName, idval, family);
				}
			}

			if (custNode != null && idNode != null)
				makeEdge(custNode, idNode, degree);

			if (custNode != null && acNode != null)
				makeEdge(custNode, acNode, degree);

		} // Each row

		return countNodes();

	}

	private int countNodes() {
		int n = nodeList.countNodes();
		logger.debug("Nbr of nodes " + n);
		return n;

	}

	/**
	 * Make an edge between two nodes if one doesn't already exist
	 * 
	 * @param src
	 * @param target
	 * @param degree
	 *            int degree, used to remove edges if exceed max size for graph
	 */

	private void makeEdge(EntityRefNode src, EntityRefNode target, int degree) {
		String key = src + ":" + target;
		if (!edgeMap.containsKey(key)) {
			edgeMap.put(key, new GenericEdge(src, target, degree));
		}
	}

	/**
	 * Look through our current nodes for any that have not been scanned
	 * 
	 * 
	 * @param degree
	 * @return
	 * @throws Exception
	 */
	private Set<EnronEntityref100> traverse(int degree) throws Exception {

		/*
		 * If our last traversal was on identifiers, we will have customer nodes
		 * that have not been scanned. This means searching on each
		 * customernumber to get a list of identifiers (several for each).
		 * 
		 * If our last traversal was on customers, we will have identifier nodes
		 * that have not been scanned. This means searching on each identifier
		 * to get a list of customers for each identifier.
		 * 
		 * Even though Indexer has a list of customers for each identifier it
		 * doesn't know the account numbers for each customer. So we do actually
		 * have to scan. However, we do know how many customers there are for
		 * each identifier. So if there is only one, or more than max edges, we
		 * can skip the scan.
		 */

		Set<EnronEntityref100> pbRows = new HashSet<EnronEntityref100>();
		Set<String> itemsToScan;
		EntityRefQuery q;

		itemsToScan = nodeList.getUnscannedValues(
				EntityRefNode.ENTITY_IDENTIFIER, 0);

		if (itemsToScan.size() > 0) {
			for (String v : itemsToScan) {
				q = new EntityRefQuery();
                                q.setMaxResult(300000);  // MFM added
				q.getAttributeList().add(
						new EntitySearchTuple<String>(
								G_SearchType.COMPARE_EQUALS,
								G_CanonicalPropertyType.ANY, v));
				List<EnronEntityref100> rows = dao.rowSearch(q);
				if (rows.size() <= maxEdgesPerNode) {
					// logger.debug("Adding " + rows.size() +
					// " rows to pbRpws that has " + pbRows.size());

					pbRows.addAll(rows);
					// logger.debug("pbRows now has " + pbRows.size());
				} else {
					// Too many links. Make a placeholder node
					Iterator<EnronEntityref100> it = rows.iterator();
					EnronEntityref100 r = it.next();
					EntityRefNode idNode = nodeFactory.makeIdentifierNode(v,
							degree);
					IdType idType = idTypeDAO.getByType(r.getIdtypeId());
					if (idType != null) {
						idNode.setId_type(idType);
						idNode.setValueType(idType.getFamily());
					}
					idNode.setPlaceholder(true);
					nodeList.addNode(idNode);
				}
			}
			nodeList.setAllScanned(EntityRefNode.ENTITY_IDENTIFIER);
		}
		logger.debug("After identifiers pbrows size is " + pbRows.size()
				+ " and nodelis size is " + nodeList.countNodes());

		itemsToScan = nodeList.getUnscannedValues(
				EntityRefNode.ENTITY_CUSTOMER, 0);
		if (itemsToScan.size() > 0) {
			q = new EntityRefQuery();
                        q.setMaxResult(300000);  // MFM added
			for (String v : itemsToScan) {
				q.getAttributeList().add(
						new EntitySearchTuple<String>(
								G_SearchType.COMPARE_EQUALS,
								G_CanonicalPropertyType.CUSTOMER_NUMBER, v));
			}
			pbRows.addAll(dao.rowSearch(q));
			nodeList.setAllScanned(EntityRefNode.ENTITY_CUSTOMER);
		}
		logger.debug("After customers pbrows size is " + pbRows.size()
				+ " and nodelis size is " + nodeList.countNodes());
		// Note that an account can have more than one customer associated with
		// it
		// TODO: optimize this by combining queries?
		itemsToScan = nodeList.getUnscannedValues(EntityRefNode.ENTITY_ACCOUNT,
				0);
		for (String v : itemsToScan) {
			q = new EntityRefQuery();
                        q.setMaxResult(300000);  // MFM added
			{
				q.getAttributeList().add(
						new EntitySearchTuple<String>(
								G_SearchType.COMPARE_EQUALS,
								G_CanonicalPropertyType.ACCOUNT, v));
			}
			List<EnronEntityref100> rows = dao.rowSearch(q);
			int n = rows.size();
			if (n <= maxEdgesPerNode)
				pbRows.addAll(rows);
			else {
				// Too many links. Make a placeholder node
				EntityRefNode acNode = nodeFactory.makeAccountNode(v, degree);
				acNode.setPlaceholder(true);
				nodeList.addNode(acNode);
			}
		}
		nodeList.setAllScanned(EntityRefNode.ENTITY_ACCOUNT);

		logger.debug("After accounts pbrows size is " + pbRows.size()
				+ " and nodelist size is " + nodeList.countNodes());
		return pbRows;
	}

	// ---------------------------------------------------------------------
	/**
	 * Remove any identifier nodes that have only one link
	 */
	private void removeLeafNodes() {
		EntityRefNode target, src;
		// First figure out which nodes are leaf nodes
		// Note that we only do this for Identifier nodes.

		logger.debug("Removing leaf nodes");

		for (GenericNode n : nodeList.getAllNodes()) {
			n.setUsed(false);
			n.setNbrLinks(0);
		}

		for (GenericEdge e : edgeMap.values()) {
			src = (EntityRefNode) e.getSourceNode();
			if (src.getEntityType() == EntityRefNode.ENTITY_IDENTIFIER) {
				src.incLinks();
				if (src.getNbrLinks() > 1 || src.isPlaceholder())
					src.setUsed(true);
			} else
				src.setUsed(true);

			target = (EntityRefNode) e.getTargetNode();
			if (target.getEntityType() == EntityRefNode.ENTITY_IDENTIFIER) {
				target.incLinks();
				if (target.getNbrLinks() > 1 || target.isPlaceholder())
					target.setUsed(true);
			} else
				target.setUsed(true);

		}

		nodeList.removeUnused();
	}

	private void removeOrphanEdges() {

		logger.debug("Removing orphan edges. Starting count " + edgeMap.size());

		Map<String, GenericEdge> temp = new HashMap<String, GenericEdge>();
		for (String key : edgeMap.keySet()) {
			GenericEdge e = edgeMap.get(key);
			if (nodeList.hasNode((EntityRefNode) e.getSourceNode())
					&& nodeList.hasNode((EntityRefNode) e.getTargetNode()))
				temp.put(key, e);
		}
		edgeMap = temp;
		logger.debug("Removed orphan edges. Ending count " + edgeMap.size());
	}

	private Set<EnronEntityref100> finishCustomers(int degree) throws Exception {

		logger.debug("Finishing customers");

		Set<EnronEntityref100> pbRows = new HashSet<EnronEntityref100>();
		Set<String> itemsToScan;

		itemsToScan = nodeList.getUnscannedValues(
				EntityRefNode.ENTITY_CUSTOMER, degree);
		if (itemsToScan.size() > 0) {
			EntityRefQuery q = new EntityRefQuery();
                        q.setMaxResult(300000);  // MFM added
			for (String v : itemsToScan) {
				q.getAttributeList().add(
						new EntitySearchTuple<String>(
								G_SearchType.COMPARE_EQUALS,
								G_CanonicalPropertyType.CUSTOMER_NUMBER, v));
			}
			pbRows.addAll(dao.rowSearch(q));

		}

		return pbRows;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see graphene.enron.services.graphserver.GraphB#setMaxNodes(int)
	 */
	@Override
	public void setMaxNodes(int maxNodes) {
		this.maxNodes = maxNodes;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see graphene.enron.services.graphserver.GraphB#setMaxEdgesPerNode(int)
	 */
	@Override
	public void setMaxEdgesPerNode(int maxEdgesPerNode) {
		this.maxEdgesPerNode = maxEdgesPerNode;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see graphene.enron.services.graphserver.GraphB#setBiPartite(boolean)
	 */
	@Override
	public void setBiPartite(boolean biPartite) {
		logger.trace("Setting biPartite to " + biPartite);
		this.biPartite = biPartite;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see graphene.enron.services.graphserver.GraphB#setShowLeafNodes(boolean)
	 */
	@Override
	public void setShowLeafNodes(boolean showLeafNodes) {
		this.showLeafNodes = showLeafNodes;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see graphene.enron.services.graphserver.GraphB#setShowNameNodes(boolean)
	 */
	@Override
	public void setShowNameNodes(boolean showNameNodes) {
		this.showNameNodes = showNameNodes;
	}

	public List<EntityRefNode> getNodesByType(char type) {
		List<EntityRefNode> results = new ArrayList<EntityRefNode>();
		for (GenericNode n : nodeList.getAllNodes()) {
			if (((EntityRefNode) n).getEntityType() == type)
				results.add((EntityRefNode) n);
		}
		return results;
	}

	/*
	 * public boolean isCustomerCentric() { return customerCentric; }
	 * 
	 * 
	 * public void setCustomerCentric(boolean customerCentric) {
	 * this.customerCentric = customerCentric; }
	 */
	/*
	 * 
	 * private String[] deDupe(String[] orig) { Set<String> set = new
	 * HashSet<String> (); for (String s:orig) set.add(s); return
	 * set.toArray(new String[0]);
	 * 
	 * }
	 */

	

	/*
	 * (non-Javadoc)
	 * 
	 * @see graphene.enron.services.graphserver.GraphB#setStyle(boolean)
	 */
	@Override
	public void setStyle(boolean style) {
		this.GQT_Style = style;
	}
	
	
}
