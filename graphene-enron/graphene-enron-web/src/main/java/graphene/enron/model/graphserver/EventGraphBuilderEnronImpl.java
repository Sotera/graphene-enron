package graphene.enron.model.graphserver;

import graphene.dao.EntityRefDAO;
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
import mil.darpa.vande.generic.V_GraphQuery;

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

	private EntityRefDAO propertyDAO;

	@Inject
	public EventGraphBuilderEnronImpl(IdTypeDAO idTypeDAO, TransactionDAO dao,
			EntityRefDAO propertyDAO) {
		super();
		this.idTypeDAO = idTypeDAO;
		this.dao = dao;
		this.propertyDAO = propertyDAO;
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
				// EntityRefQuery eq = new EntityRefQuery();
				// G_SearchTuple<String> est = new G_SearchTuple<>();
				// est.setValue(s_acno);
				// est.setSearchType(G_SearchType.COMPARE_EQUALS);
				// est.setFamily(G_CanonicalPropertyType.ACCOUNT);
				// eq.addAttribute(est);
				//List listOfProperties = propertyDAO.findByQuery(eq);
				
				
				src = new V_GenericNode(s_acno);
				src.setIdType("account");
				src.setFamily(G_CanonicalPropertyType.ACCOUNT.getValueString());
				src.setIdVal(s_acno);
				src.setValue(s_acno);
				src.setLabel(s_acname);
				src.setColor("#F08080");
				//for (s)
				// value type is "customer"
				src.addProperty("Account Number", s_acno);
				src.addProperty("Account Owner", s_acname);
				src.addProperty("background-color", "red");
				src.addProperty("color", "red");

				unscannedNodeList.add(src);
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
				target.setLabel(t_acname);
				src.setColor("#22FF22");
				// value type is "customer"
				target.addProperty("Account Number", t_acno);
				target.addProperty("Account Owner", t_acname);
				target.addProperty("color", "red");

				unscannedNodeList.add(target);
				nodeList.addNode(target);
			}

		}

		if (src != null && target != null) {
			//Here, an event id is used, so we will get an edger per event.
			String key = generateEdgeId(p.getPairId().toString());
			
			if (key != null && !edgeMap.containsKey(key)) {
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
			}else{
				//Handle how multiple edges are aggregated.
			}

		}

		return true;
	}


}
