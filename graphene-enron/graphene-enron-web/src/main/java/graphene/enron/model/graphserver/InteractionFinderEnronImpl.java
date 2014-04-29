package graphene.enron.model.graphserver;

import graphene.dao.TransactionDAO;
import graphene.enron.model.sql.enron.EnronTransactionPair100;
import graphene.model.query.EventQuery;
import graphene.util.CallBack;

import java.util.List;

import mil.darpa.vande.InteractionFinder;
import mil.darpa.vande.generic.V_Actor;
import mil.darpa.vande.generic.V_GraphQuery;
import mil.darpa.vande.interactions.Interaction;
import mil.darpa.vande.interactions.InteractionCallback;

public class InteractionFinderEnronImpl implements InteractionFinder,
		CallBack<EnronTransactionPair100> {

	private TransactionDAO<EnronTransactionPair100, EventQuery> dao;
	private InteractionCallback cb = null;

	public InteractionFinderEnronImpl(
			TransactionDAO<EnronTransactionPair100, EventQuery> dao) {
		this.dao = dao;
	}

	@Override
	public long countEdges(V_GraphQuery originalQuery, String id) {
		long n = 0;
		try {
			n = dao.countEdges(id);
		} catch (Exception e) {
			e.printStackTrace();
		} // TODO: could limit to the original query parameters

		return n;
	}

	@Override
	public void query(List<String> idList, V_GraphQuery originalQuery,
			InteractionCallback cb) {

		this.cb = cb;
		EventQuery q = new EventQuery();

		for (String s : idList)
			q.addId(s);

		dao.performThrottlingCallback(0, 0, this, q);

		/*
		 * List<EnronTransactionPair100> results = null;
		 * 
		 * try { results = dao.findByQuery(0, 0, q); } catch (Exception e) { //
		 * TODO Auto-generated catch block e.printStackTrace(); return; }
		 * 
		 * for (EnronTransactionPair100 p:results) { Interaction ia =
		 * makeInteraction(p); cb.callBack(ia); }
		 */
	}

	private Interaction makeInteraction(EnronTransactionPair100 p) {

		String s_acno = p.getSenderId().toString();
		String s_acname = p.getSenderValueStr();

		String t_acno = p.getReceiverId().toString();
		String t_acname = p.getReceiverValueStr();

		// Make Sender Actor

		V_Actor src = new V_Actor(s_acno);
		src.setLabel(p.getSenderValueStr());
		src.setIdType("account");
		src.setIdVal(s_acno);
		src.addProperty("Account Number", s_acno);
		src.addProperty("Account Owner", s_acname);
		src.addProperty("Customer Number", p.getSenderId().toString());

		// Make Receiver Actor
		V_Actor target = new V_Actor(t_acno);
		target.setLabel(t_acno);
		target.setIdType("account");
		target.setIdVal(t_acno);
		target.addProperty("Account Number", t_acno);
		target.addProperty("Account Owner", t_acname);
		target.addProperty("Customer Number", p.getReceiverId().toString());

		// Make the Interaction
		long dt = p.getTrnDt().getTime();
		double v = p.getTrnValueNbr().doubleValue();
		Interaction ia = new Interaction(src, target, dt, v);
		return ia;
	}

	@Override
	public boolean callBack(EnronTransactionPair100 t) {
		Interaction ia = makeInteraction(t);
		cb.callBack(ia);
		return true;
	}

}
