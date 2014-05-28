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
import graphene.model.query.EventQuery;
import graphene.model.query.StringQuery;
import graphene.util.G_CallBack;
import graphene.util.validator.ValidationUtils;

import java.util.List;

import org.apache.tapestry5.ioc.annotations.Inject;
import org.slf4j.Logger;

import mil.darpa.vande.generic.V_GenericEdge;
import mil.darpa.vande.generic.V_GenericGraph;
import mil.darpa.vande.generic.V_GenericNode;
import mil.darpa.vande.generic.V_GraphQuery;
import mil.darpa.vande.generic.V_NodeList;
import mil.darpa.vande.property.PropertyFinder;
import mil.darpa.vande.property.V_PropertyCallback;

public class PropertyFinderEnronImpl implements PropertyFinder,
		G_CallBack<EnronEntityref100> {
	public static final char ENTITY_ACCOUNT = 'A';
	public static final char ENTITY_CUSTOMER = 'C';
	public static final char ENTITY_IDENTIFIER = 'I';

	private V_PropertyCallback cb = null;

	private IdTypeDAO<EnronIdentifierType100, StringQuery> idTypeDAO;

	@Inject
	private Logger logger;

	private V_NodeList nodeList;

	private EntityRefDAO<EnronEntityref100, EntityRefQuery> propertyDAO;

	public PropertyFinderEnronImpl(
			EntityRefDAO<EnronEntityref100, EntityRefQuery> dao,
			IdTypeDAO idTypeDAO) {
		this.propertyDAO = dao;
		this.idTypeDAO = idTypeDAO;
	}

	@Override
	public boolean callBack(EnronEntityref100 t) {
		cb.callBack(makeInteraction(t));
		return true;
	}

	@Override
	public long countEdges(String id) {
		long n = 0;
		try {
			n = propertyDAO.countEdges(id);
		} catch (Exception e) {
			e.printStackTrace();
		} // TODO: could limit to the original query parameters

		return n;
	}

	private V_GenericGraph makeInteraction(EnronEntityref100 p) {

		// XXX:What we really want is just a list of Actors, which is like an
		// interaction (except an Interaction has only two actors)
		// But we'll use a node list in a pinch. This is the one we'll return.
		V_GenericGraph subgraph = new V_GenericGraph();

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
			subgraph.addNode(custNode);
		}
		if (ValidationUtils.isValid(acno)) {
			acnoNode = new V_GenericNode(acno);
			acnoNode.setIdType(ENTITY_ACCOUNT);
			acnoNode.setIdVal(acno);
			acnoNode.setLabel(acno);
			subgraph.addNode(acnoNode);
		}

		if (ValidationUtils.isValid(idval)) {
			idNode = new V_GenericNode(idval);
			idNode.setIdType(ENTITY_IDENTIFIER);
			idNode.setIdVal(idval);
			idNode.setLabel(idval);
			subgraph.addNode(idNode);
		}
		if (custNode != null && idNode != null) {
			V_GenericEdge v = new V_GenericEdge(custNode, idNode);
			v.setIdType(G_RelationshipType.RELATED_TO_ID.name());
			subgraph.addEdge(v);
		}

		if (custNode != null && acnoNode != null) {
			V_GenericEdge v = new V_GenericEdge(custNode, acnoNode);
			v.setIdType(G_RelationshipType.RELATED_TO_ID.name());
			subgraph.addEdge(v);
		}

		/*
		 * How to handle this.
		 * 
		 * Thoughts.
		 * 
		 * Create a node list in this method that gets returned as a value. But
		 * before returning, add the local node list to the field level node
		 * list.
		 * 
		 * After this method, a callback is going to be performed on whatever we
		 * return, most likely to drill down (my understanding)
		 * 
		 * 
		 * Update: Maybe what we really need to pass back to the graph builder
		 * is a subgraph. No where else do we know better the way the property
		 * graph should be built, except for here.
		 * 
		 * (Unless we construct an object that represents our ideal ingest
		 * format, and expect that all property graphs will be alike.
		 * 
		 * Maybe offer both?)
		 */
		// nodeList.add(localNodeList);

		// Interaction ia = new Interaction(src, target, dt, v);
		return subgraph;
	}

	@Override
	public void query(List<String> idList, V_GraphQuery q, V_PropertyCallback cb) {
		this.cb = cb;
		EntityRefQuery eq = new EntityRefQuery();

		// add to attribute list
		for (String id : idList) {
			eq.getAttributeList().add(
					new EntitySearchTuple<String>(G_SearchType.COMPARE_EQUALS,
							G_CanonicalPropertyType.ANY, id));
		}
		logger.debug("EntityRefQuery=" + eq.toString());
		propertyDAO.performCallback(0, 0, this, eq);
	}

}
