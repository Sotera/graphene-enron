package graphene.enron.model.graphserver;

import graphene.dao.EntityRefDAO;
import graphene.dao.IdTypeDAO;
import graphene.enron.model.sql.enron.EnronEntityref100;
import graphene.model.idl.G_CanonicalPropertyType;
import graphene.model.idl.G_CanonicalRelationshipType;
import graphene.model.idl.G_EdgeType;
import graphene.model.idl.G_EdgeTypeAccess;
import graphene.model.idl.G_NodeTypeAccess;
import graphene.model.idl.G_PropertyKeyTypeAccess;
import graphene.model.query.StringQuery;
import graphene.services.PropertyGraphBuilder;
import graphene.util.validator.ValidationUtils;
import mil.darpa.vande.generic.V_GenericEdge;
import mil.darpa.vande.generic.V_GenericNode;

import org.apache.avro.AvroRemoteException;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.slf4j.Logger;

/**
 * This replaces the finder/builder implementations, which were developed for
 * interaction graphs.
 * 
 * @author djue
 * 
 */
public class PropertyGraphBuilderEnronImpl extends
		PropertyGraphBuilder<EnronEntityref100> {

	private IdTypeDAO<EnronEntityref100, StringQuery> idTypeDAO;
	@Inject
	private G_EdgeTypeAccess edgeTypeAccess;

	@Inject
	private G_NodeTypeAccess nodeTypeAccess;

	@Inject
	private G_PropertyKeyTypeAccess propertyKeyTypeAccess;
	@Inject
	Logger logger;

	@Inject
	public PropertyGraphBuilderEnronImpl(IdTypeDAO idTypeDAO,
			EntityRefDAO propertyDAO) {
		super();
		this.idTypeDAO = idTypeDAO;
		this.dao = propertyDAO;
		this.supportedDatasets.add("Enron");
	}

	/**
	 * This callback just creates the nodes and edges from a single row.
	 */
	@Override
	public boolean callBack(EnronEntityref100 p) {

		String custno = p.getCustomernumber();
		String acno = p.getAccountnumber();
		String identifier = p.getIdentifier();

		V_GenericNode custNode = null, acnoNode = null, idNode = null;
		if (ValidationUtils.isValid(custno)) {
			custNode = nodeList.getNode(custno);
			if (custNode == null) {
				custNode = new V_GenericNode(custno);
				custNode.setIdType("customer");
				custNode.setFamily(G_CanonicalPropertyType.CUSTOMER_NUMBER
						.name());
				custNode.setIdVal(custno);
				custNode.setValue(custno);
				custNode.setLabel(custno);
				// value type is "customer"
				custNode.addProperty("Customer Number", custno);
				custNode.addProperty("background-color", "red");
				custNode.addProperty("color", "red");

				/*
				 * This is kind of business logic-like. The customer node also
				 * gets any id properties baked in.
				 */
				if (ValidationUtils.isValid(identifier)) {
					int idTypeId = p.getIdtypeId();
					custNode.addProperty("ShortName",
							idTypeDAO.getShortName(idTypeId));
					custNode.addProperty("Value", identifier);
					custNode.addProperty("Family",
							idTypeDAO.getNodeType(idTypeId));

				}
				unscannedNodeList.add(custNode);
				nodeList.addNode(custNode);
			}

		}

		if (ValidationUtils.isValid(acno)) {
			acnoNode = nodeList.getNode(acno);
			if (acnoNode == null) {
				acnoNode = new V_GenericNode(acno);
				// logger.debug("Adding account node with value " + acno);
				acnoNode.setIdType(idTypeDAO.getNodeType(p.getIdtypeId()));
				acnoNode.setFamily(G_CanonicalPropertyType.ACCOUNT.name());
				acnoNode.setIdVal(acno);
				acnoNode.setValue(acno);
				acnoNode.setLabel(acno);
				acnoNode.addProperty("background-color", "Lime");
				acnoNode.addProperty("color", "Lime");
				unscannedNodeList.add(acnoNode);
				nodeList.addNode(acnoNode);
			}
		}

		if (ValidationUtils.isValid(identifier, p.getIdtypeId())) {
			String nodeId = identifier + p.getIdtypeId();
			idNode = nodeList.getNode(nodeId);
			String idFamily = idTypeDAO.getNodeType(p.getIdtypeId());
			String commonType = idNode.getNodeType();
			if (idNode == null) {
				idNode = new V_GenericNode(nodeId);
				// logger.debug("Adding identifier node with value " + key);
				acnoNode.setFamily(commonType);
				idNode.setIdType(idFamily);
				idNode.setIdVal(identifier);
				idNode.setValue(identifier);
				idNode.setLabel(identifier);
				idNode.addProperty(idFamily, identifier);
				if (custNode != null) {
					// also add it to the customer, this is a legacy thing to
					// embed more data in the important nodes. --djue
					custNode.addProperty(idFamily, identifier);
				}
				if (commonType.equals(G_CanonicalPropertyType.PHONE.name())) {
					idNode.addProperty("color", "green");
				}
				if (commonType.equals(G_CanonicalPropertyType.EMAIL_ADDRESS
						.name())) {
					idNode.addProperty("color", "aqua");
				}
				if (commonType.equals(G_CanonicalPropertyType.ADDRESS.name())) {
					idNode.addProperty("color", "gray");
				}
				unscannedNodeList.add(idNode);
				nodeList.addNode(idNode);
			}
			if (custNode != null && idNode != null) {
				String key = generateEdgeId(custNode.getId(), idNode.getId());
				try {
					if (key != null && !edgeMap.containsKey(key)) {
						V_GenericEdge v = new V_GenericEdge(idNode, custNode);

						G_EdgeType ownerOf = edgeTypeAccess
								.getEdgeType(G_CanonicalRelationshipType.OWNER_OF
										.name());
						G_EdgeType edgeType = edgeTypeAccess
								.getEdgeType(G_CanonicalRelationshipType.HAS_ID
										.name());
						if (commonType.equals(G_CanonicalPropertyType.PHONE
								.name())) {
							edgeType = edgeTypeAccess
									.getEdgeType(G_CanonicalRelationshipType.COMMUNICATION_ID_OF
											.name());

						} else if (commonType
								.equals(G_CanonicalPropertyType.EMAIL_ADDRESS
										.name())) {
							edgeType = edgeTypeAccess
									.getEdgeType(G_CanonicalRelationshipType.COMMUNICATION_ID_OF
											.name());

						} else if (commonType
								.equals(G_CanonicalPropertyType.ADDRESS.name())) {
							edgeType = edgeTypeAccess
									.getEdgeType(G_CanonicalRelationshipType.ADDRESS_OF
											.name());
						}
						v.setIdType(edgeType.getName());
						v.setIdVal(edgeType.getName());
						v.setLabel(null);

						v.addData("Relationship type", ownerOf.getName());
						v.addData("Source Column",
								p.getIdentifiercolumnsource());
						v.addData("Source Table", p.getIdentifiertablesource());
						edgeMap.put(key, v);
					}
				} catch (AvroRemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		if (custNode != null && acnoNode != null) {
			String key = generateEdgeId(custNode.getId(), acnoNode.getId());
			if (!edgeMap.containsKey(key)) {
				V_GenericEdge v = new V_GenericEdge(custNode, acnoNode);
				v.setIdType(G_CanonicalRelationshipType.OWNER_OF.name());
				v.setLabel(null);
				v.setIdVal(G_CanonicalRelationshipType.OWNER_OF.name());
				v.addData("Relationship type",
						G_CanonicalRelationshipType.OWNER_OF.name());
				v.addData("Source Column", p.getIdentifiercolumnsource());
				v.addData("Source Table", p.getIdentifiertablesource());

				edgeMap.put(key, v);
			}
		}

		return true;
	}

}
