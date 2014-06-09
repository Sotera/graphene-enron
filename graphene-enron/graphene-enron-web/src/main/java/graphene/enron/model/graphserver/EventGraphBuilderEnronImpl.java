package graphene.enron.model.graphserver;

import graphene.dao.IdTypeDAO;
import graphene.dao.TransactionDAO;
import graphene.enron.model.sql.enron.EnronIdentifierType100;
import graphene.enron.model.sql.enron.EnronTransactionPair100;
import graphene.model.idl.G_CanonicalPropertyType;
import graphene.model.idl.G_RelationshipType;
import graphene.model.query.StringQuery;
import graphene.services.EventGraphBuilder;
import graphene.util.validator.ValidationUtils;
import mil.darpa.vande.generic.V_GenericEdge;
import mil.darpa.vande.generic.V_GenericNode;

import org.apache.tapestry5.ioc.annotations.Inject;
import org.slf4j.Logger;

/**
 * This replaces the finder/builder implementations, which were developed for
 * interaction graphs.
 * 
 * @author djue
 * 
 */
public class EventGraphBuilderEnronImpl extends
		EventGraphBuilder<EnronTransactionPair100> {

	private IdTypeDAO<EnronIdentifierType100, StringQuery> idTypeDAO;

	@Inject
	Logger logger;

	@Inject
	public EventGraphBuilderEnronImpl(IdTypeDAO idTypeDAO,
			TransactionDAO propertyDAO) {
		super();
		this.idTypeDAO = idTypeDAO;
		this.dao = propertyDAO;
		this.supportedDatasets.add("Enron");
	}

	/**
	 * This callback just creates the nodes and edges from a single row.
	 */
	@Override
	public boolean callBack(EnronTransactionPair100 p) {

		String s_acno = p.getSenderId().toString();
		String s_acname = p.getSenderValueStr();

		String t_acno = p.getReceiverId().toString();
		String t_acname = p.getReceiverValueStr();

		V_GenericNode src = null, target = null;
		if (ValidationUtils.isValid(s_acno)) {
			src = nodeList.getNode(s_acno);
			if (src == null) {
				src = new V_GenericNode(s_acno);
				src.setIdType("account");
				src.setFamily(G_CanonicalPropertyType.ACCOUNT.getValueString());
				src.setIdVal(s_acno);
				src.setValue(s_acno);
				src.setLabel(s_acno);
				// value type is "customer"
				src.addProperty("Account Number", s_acno);
				src.addProperty("Account Owner", s_acname);
				src.addProperty("background-color", "red");
				src.addProperty("color", "red");

				newNodeList.add(src);
				nodeList.addNode(src);
			}

		}
		if (ValidationUtils.isValid(t_acno)) {
			target = nodeList.getNode(t_acno);
			if (target == null) {
				target = new V_GenericNode(t_acno);
				target.setIdType("account");
				target.setFamily(G_CanonicalPropertyType.ACCOUNT
						.getValueString());
				target.setIdVal(t_acno);
				target.setValue(t_acno);
				target.setLabel(t_acno);
				// value type is "customer"
				target.addProperty("Account Number", t_acno);
				target.addProperty("Account Owner", t_acname);
				target.addProperty("color", "red");

				newNodeList.add(target);
				nodeList.addNode(target);
			}

		}

		if (src != null && target != null) {
			String key = generateEdgeId(p.getPairId().toString());
			if (!edgeMap.containsKey(key)) {
				V_GenericEdge v = new V_GenericEdge(src, target);
				v.setIdType(G_RelationshipType.HAS_ACCOUNT.name());
				v.setLabel(G_RelationshipType.HAS_ACCOUNT.name());
				v.setIdVal(G_RelationshipType.HAS_ACCOUNT.name());
				long dt = p.getTrnDt().getTime();
				double value = p.getTrnValueNbr();
				v.setDoubleValue(value);

				v.addData("date", Long.toString(dt));
				v.addData("amount", Double.toString(value));
				v.addData("id", p.getPairId().toString());
				edgeMap.put(key, v);
			}

		}

		return true;
	}

}
