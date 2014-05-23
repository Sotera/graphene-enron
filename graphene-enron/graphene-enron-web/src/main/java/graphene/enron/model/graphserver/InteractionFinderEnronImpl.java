package graphene.enron.model.graphserver;

import graphene.dao.TransactionDAO;
import graphene.enron.model.sql.enron.EnronTransactionPair100;
import graphene.model.query.EventQuery;
import graphene.util.G_CallBack;

import java.util.List;

import org.apache.tapestry5.ioc.annotations.Inject;

import mil.darpa.vande.generic.V_Actor;
import mil.darpa.vande.generic.V_GraphQuery;
import mil.darpa.vande.interactions.Interaction;
import mil.darpa.vande.interactions.InteractionFinder;
import mil.darpa.vande.interactions.V_InteractionCallback;

public class InteractionFinderEnronImpl implements InteractionFinder,
		G_CallBack<EnronTransactionPair100> {

	private TransactionDAO<EnronTransactionPair100, EventQuery> dao;

	// FIXME: It's probably not a good idea that this is potentially modified
	// with every call.
	private V_InteractionCallback cb = null;

	@Inject
	public InteractionFinderEnronImpl(
			TransactionDAO<EnronTransactionPair100, EventQuery> dao) {
		this.dao = dao;
	}

	@Override
	public long countEdges(V_GraphQuery q, String id) {
		long n = 0;
		try {
			n = dao.countEdges(id);
		} catch (Exception e) {
			e.printStackTrace();
		} // TODO: could limit to the original query parameters

		return n;
	}

	@Override
	public void query(List<String> idList, V_GraphQuery q,
			V_InteractionCallback cb) {

		this.cb = cb;
		EventQuery eq = new EventQuery();
		eq.addIds(idList);
		dao.performCallback(0, 0, this, eq);
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
		//target.setLabel(t_acno);
		target.setLabel(p.getReceiverValueStr());
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
		// Now call the 'external' callback
		// TODO: Once this the other non interaction code is refactored to work
		// like this (and works) rethink the logic to see if this is really the
		// elegant/simple way to do this.--djue
		cb.callBack(ia);
		return true;
	}

}
