package graphene.enron.web.rest;

import graphene.dao.TransferDAO;
import graphene.model.query.EventQuery;
import graphene.model.view.events.SingleSidedEventRow;
import graphene.model.view.events.SingleSidedEvents;
import graphene.rest.ws.LedgerFreeTextRS;

import java.util.List;

import org.apache.tapestry5.ioc.annotations.InjectService;

public class LedgerFreeTextRSImpl implements LedgerFreeTextRS {

	//TODO: Revisit the caching needs.  This is not how the pros do it. --djue
	//private LedgerQuery prevQuery;

	@InjectService("Solr")
	private TransferDAO<SingleSidedEventRow, EventQuery> solr;

	@Override
	public SingleSidedEvents getTransactions(String account, int start,
			int limit, String minAmount, String maxAmount, String fromdtSecs,
			String todtSecs, String comments, String sortColumn) {

		EventQuery q = new EventQuery();
		q.addId(account);
		q.setFirstResult(start);
		q.setMaxResult(limit);
		q.setMinAmount(Double.parseDouble(minAmount.isEmpty() ? "0" : minAmount));
		q.setMaxAmount(Double.parseDouble(maxAmount.isEmpty() ? "0" : maxAmount));

		q.setMinSecs(Long.parseLong(fromdtSecs.isEmpty() ? "0" : fromdtSecs));
		q.setMaxSecs(Long.parseLong(todtSecs.isEmpty() ? "0" : todtSecs));

		// FIXME: we probably want this by default, but it should be a parameter
		q.setFindRelatedIds(true);

		q.setComments(comments);
		q.setSortAndDirection(sortColumn);
		List<SingleSidedEventRow> results;
		SingleSidedEvents lt = new SingleSidedEvents();
		try {
			results = solr.findByQuery(q.getFirstResult(), q.getMaxResult(), q);
			lt.setMultiUnit(true);
			lt.setRows(results);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// TODO: If we want any app-generated stats on ledgers that were read
		// in, do it here.
		return lt;
	}

}
