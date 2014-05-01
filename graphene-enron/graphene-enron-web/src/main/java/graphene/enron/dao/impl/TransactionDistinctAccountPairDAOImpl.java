package graphene.enron.dao.impl;

import graphene.dao.TransactionDAO;
import graphene.dao.TransactionDistinctPairDAO;
import graphene.dao.sql.GenericDAOJDBCImpl;
import graphene.enron.model.sql.enron.EnronTransactionPair100;
import graphene.model.query.EventQuery;
import graphene.util.CallBack;
import graphene.util.stats.TimeReporter;
import graphene.util.validator.ValidationUtils;

import java.util.ArrayList;
import java.util.List;

import mil.darpa.vande.legacy.entity.IdProperty;
import mil.darpa.vande.legacy.graphserver.TransactionDistinctAccountPair;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionDistinctAccountPairDAOImpl extends
		GenericDAOJDBCImpl<TransactionDistinctAccountPair, EventQuery>
		implements
		TransactionDistinctPairDAO<TransactionDistinctAccountPair, EventQuery>

{

	@Inject
	private TransactionDAO<EnronTransactionPair100, EventQuery> transferDAO;

	static Logger logger = LoggerFactory
			.getLogger(TransactionDistinctAccountPairDAOImpl.class);
	static String getReceiversSql = "SELECT acct_nbr_sender, acct_nbr_receiver,"
			+ " customer_nbr_sender,"
			+ " customer_nbr_receiver,"
			+ " acct_nm_sender,"
			+ " acct_nm_receiver, "
			+ " nbr, total"

			+ " from Enron_transaction_pair_1_00 S"
			+ " RIGHT JOIN ( select"

			+ " count(acct_nbr_sender) as nbr,"
			+ " sum(amt_debit+amt_credit) as total, "
			+ " max(pair_id) as maxpair"

			+ " from Enron_transaction_pair_1_00"
			+ " where acct_nbr_sender = ?"
			+ " group by acct_nbr_sender,acct_nbr_receiver) A"
			+ " ON S.pair_id = A.maxpair";

	static String getSendersSql = "SELECT acct_nbr_sender, acct_nbr_receiver,"
			+ " customer_nbr_sender," + " customer_nbr_receiver,"
			+ " acct_nm_sender," + " acct_nm_receiver, " + " nbr, total"
			+ " from Enron_transaction_pair_1_00 S" + " RIGHT JOIN ( select"

			+ " count(acct_nbr_sender) as nbr,"
			+ " sum(amt_debit+amt_credit) as total,"
			+ " max(pair_id) as maxpair"

			+ " from Enron_transaction_pair_1_00"
			+ " where acct_nbr_receiver = ?"
			+ " group by acct_nbr_sender,acct_nbr_receiver) A"
			+ " ON S.pair_id = A.maxpair";

	static String getEitherSql = getReceiversSql + " UNION " + getSendersSql;

	@Override
	public List<TransactionDistinctAccountPair> pairQuery(EventQuery q)
			throws Exception {
		// Search for transactions where one participant matches the query and
		// construct a TransactionObject for each distinct counterpart

		// TODO FIXME: Use the Unified Transactions table instead when it is
		// fixed and we have a dao.

		// NB we can't use 'where sender == foo or receiver == foo' because we
		// need
		// to know the separate totals in each direction. So we end up with two
		// objects for each distinct pair - one for A to B and one for B to A

		List<TransactionDistinctAccountPair> results = new ArrayList<TransactionDistinctAccountPair>();
		for (String ac : q.getIdList()) {
			List<TransactionDistinctAccountPair> list = getResults(ac, q,
					false, false);
			if (ValidationUtils.isValid(list)) {
				results.addAll(list);
			}else{
				logger.error("Search returned a null result");
			}
		}
		return results;
	}

	@Override
	public List<TransactionDistinctAccountPair> getDestFor(String nbr,
			EventQuery q) throws Exception {
		return getResults(nbr, q, true, false);
	}

	private List<TransactionDistinctAccountPair> getResults(String nbr,
			EventQuery originalQuery, boolean matchSender, boolean matchReceiver)
			throws Exception {
		if (!NumberUtils.isDigits(nbr)) {
			logger.error("String provided could not be turned into a number as needed: "
					+ nbr);
			return null;
		}
		TimeReporter t = new TimeReporter("getResults", logger);

		Long srcnum = new Long(nbr);
		List<TransactionDistinctAccountPair> results = new ArrayList<TransactionDistinctAccountPair>();

		// Start by using our standard ledger reader to get all matches
		EventQuery q = new EventQuery();
		q.addId(nbr);
		List<EnronTransactionPair100> entries;
		entries = transferDAO.findByQuery(0, 1000000, q);

		// At this point we have transactions where either the sender or the
		// receiver matches "nbr"
		// TODO: restrict the query to the party we are interested in
		logger.debug("All entries count " + entries.size());
		for (EnronTransactionPair100 e : entries) {

			Long s = e.getSenderId();
			Long d = e.getReceiverId();

			if (matchSender) {
				if (s != srcnum)
					continue;
			} else if (matchReceiver) {
				if (d != srcnum)
					continue;
			}
			updateList(results, e);
		}

		for (TransactionDistinctAccountPair tp : results) {
			tp.addEdgeAttribute(new IdProperty("Total Bytes ", Double
					.toString(tp.getTotal())));
			tp.addEdgeAttribute(new IdProperty("Nbr Emails", Integer
					.toString(tp.getNbr())));
		}
		t.logAsCompleted();
		return results;

	}

	@Override
	public List<TransactionDistinctAccountPair> getSrcFor(String nbr,
			EventQuery q) throws Exception {

		{
			return getResults(nbr, q, false, true);
			// return doQuery(getSendersSql, nbr, null);
		}
	}

	/**
	 * Add a transaction to a list of distinct transactions
	 * 
	 * @param list
	 * @param entry
	 */
	private void updateList(List<TransactionDistinctAccountPair> list,
			EnronTransactionPair100 entry) {

		for (TransactionDistinctAccountPair p : list) {
			Long src = new Long(p.getSrc());
			Long dest = new Long(p.getSrc());

			if ((src == entry.getSenderId()) && (dest == entry.getReceiverId())) {
				p.setNbr(p.getNbr() + 1);
				double am = p.getTotal();
				am += entry.getTrnValueNbr().doubleValue();
				p.setTotal(am);
				return;
			}
		}
		// a new one
		TransactionDistinctAccountPair tp = makePair(entry);
		list.add(tp);
	}

	private TransactionDistinctAccountPair makePair(
			EnronTransactionPair100 entry) {
		TransactionDistinctAccountPair tp = new TransactionDistinctAccountPair();
		tp.setNbr(1);
		tp.setSrc(entry.getSenderId().toString());
		tp.setDest(entry.getReceiverId().toString());
		double am = entry.getTrnValueNbr().doubleValue();
		tp.setTotal(am);
		tp.addSrcAttribute(new IdProperty("IdentifierType", "account"));
		tp.addDestAttribute(new IdProperty("IdentifierType", "account"));
		tp.addEdgeAttribute(new IdProperty("IdentifierType",
				"Unique Email Pairs"));
		tp.addEdgeAttribute(new IdProperty("Identifier", entry
				.getSenderValueStr() + " -> " + entry.getReceiverValueStr()));
		tp.addSrcAttribute(new IdProperty("entityName", entry
				.getSenderValueStr()));
		tp.addDestAttribute(new IdProperty("entityName", entry
				.getReceiverValueStr()));
		return tp;
	}

	@Override
	public List<TransactionDistinctAccountPair> findByQuery(long offset,
			long maxResults, EventQuery q) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<TransactionDistinctAccountPair> getAll(long offset,
			long maxResults) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long count(EventQuery q) throws Exception {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean performCallback(long offset, long maxResults,
			CallBack<TransactionDistinctAccountPair> cb, EventQuery q) {
		// TODO Auto-generated method stub
		return false;
	}

}
