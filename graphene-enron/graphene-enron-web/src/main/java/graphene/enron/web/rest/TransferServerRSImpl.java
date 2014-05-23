package graphene.enron.web.rest;

import graphene.dao.TransactionDAO;
import graphene.enron.model.sql.enron.EnronTransactionPair100;
import graphene.enron.model.view.transferserver.TransferRowFunnel;
import graphene.model.query.EventQuery;
import graphene.model.view.events.EventStatistics;
import graphene.model.view.events.DirectedEvents;
import graphene.model.view.events.DirectedEventRow;
import graphene.rest.ws.TransferServerRS;
import graphene.util.FastNumberUtils;
import graphene.util.stats.TimeReporter;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.QueryParam;

import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.joda.time.DateTime;
import org.slf4j.Logger;

/**
 * The services that let you search for a Ledger This is the only service
 * returning JSON. See LedgerXLS and LedgerCVS for other available formats.
 * 
 * @author pgofton
 * 
 */

public class TransferServerRSImpl implements TransferServerRS {
	private TransferRowFunnel funnel = new TransferRowFunnel();
	@Inject
	private Logger logger;

	@Persist
	private List<DirectedEventRow> prevAllRows;

	@Persist
	private EventQuery prevQuery;

	@Persist
	private EventStatistics prevStatistics;

	@Inject
	private TransactionDAO<EnronTransactionPair100, EventQuery> transferDAO;

	@Override
	public DirectedEvents getEvents(String account, int start, int limit,
			String minAmount, String maxAmount, String minSecs, String maxSecs,
			String comments, String sortColumn) {
		TimeReporter t = new TimeReporter("getEvents", logger);
		if (start < 0) {
			logger.debug("Got Enron Transfers request with invalid starting offset "
					+ start);
			return new DirectedEvents(); // bug in extjs often asks for
											// negative start
		}

		EventQuery q = new EventQuery();
		q.addId(account);
		q.setFirstResult(start);
		q.setMaxResult(limit);
		q.setMinSecs(FastNumberUtils.parseLongWithCheck(minSecs, 0));
		q.setMaxSecs(FastNumberUtils.parseLongWithCheck(maxSecs, 0));
		q.setMinAmount(Double.parseDouble(minAmount.isEmpty() ? "0" : minAmount));
		q.setMaxAmount(Double.parseDouble(maxAmount.isEmpty() ? "0" : maxAmount));
		q.setComments(comments);
		q.setSortAndDirection(sortColumn);
		DirectedEvents transactions = new DirectedEvents();
		List<DirectedEventRow> allRows = null;
		try {
			// TODO: not working?
			// if (q.equalsIgnoreLimits(prevQuery)) {
			// logger.debug("Same as previous query - no need to search");
			// allRows = prevAllRows;
			// } else
			if (account.contains(",")) {

				allRows = processIntersections(account, transactions);

			} else if (q.isSingleId()
					&& (q.getComments() == null || q.getComments().length() == 0)) {
				allRows = processSingleAccount(q, transactions);
				if (transactions.isMultiUnit()) {
					for (DirectedEventRow r : allRows)
						r.setBalance(0);
				}
				// updateBalances(allRows);
			}
			// Note that multi account means searching across multiple accounts
			// Showing the intersection of multiple accounts is
			else {
				allRows = processMultiAccount(q);
			}
			transactions.setResultCount(allRows.size());

			List<DirectedEventRow> subSet = slice(allRows, q.getFirstResult(),
					q.getMaxResult());
			transactions.setRows(subSet);

			prevQuery = q;
			prevAllRows = allRows;

			logger.debug("Returning " + transactions.rows.size() + " rows");
			logger.debug("Out of total count " + allRows.size());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		t.logElapsed();
		return transactions;

	}

	/**
	 * Process query for a single account. Note that there may actually be more
	 * than one account in the result set, since we include the earlier and
	 * later accounts when the account transitioned. We simultaneously populate
	 * a monthly statistics object and persist it in the session for subsequent
	 * retrieval. Daily statistics are only generated on demand.
	 * 
	 * @param q
	 *            Query object
	 * @param transactions
	 *            SingleSidedEvents object
	 * @return List of LedgerPairRows matching the query
	 * @throws Exception
	 */
	private List<DirectedEventRow> processSingleAccount(EventQuery q,
			DirectedEvents transactions) throws Exception {
		List<EnronTransactionPair100> entries;
		List<DirectedEventRow> rows;
		double localUnitBalance = 0;
		double unitBalance = 0;

		logger.debug("Starting process single account with query "
				+ q.toString());

		// We now get all the transactions if it's a single account, so that we
		// can get
		// the statistics

		EventQuery fullQ = new EventQuery();
		fullQ.setIdList(q.getIdList());

		// MFM have to preserve the sort column and direction
		String sortCol = q.getSortColumn();
		String sortDir = (q.isSortAscending()) ? "" : "$";
		sortCol = sortCol + sortDir;
		fullQ.setSortAndDirection(sortCol);

		entries = transferDAO.findByQuery(fullQ); // *** MFM REVISIT
													// NAME:
													// transferDAOfullQ
		// TODO: Check if we need to dedupe here, before calculating statistics.
		rows = new ArrayList<DirectedEventRow>();

		boolean multiUnit = false;
		String lastUnit = null;
		boolean transitionFound = false;

		for (EnronTransactionPair100 e : entries) {
			if (lastUnit == null)
				lastUnit = e.getTrnValueNbrUnit();
			else if (!lastUnit.equals(e.getTrnValueNbrUnit()))
				multiUnit = true;

			// At the end of 2010 all accounts were transitioned to new ones.
			// Although the new account received an opening balance credit
			// transaction,
			// the old account did not get a closing balance debit.
			// The following code allows for zeroing out the account at the
			// start of the year.
			DateTime dt = new DateTime(e.getTrnDt());

			if (!transitionFound && dt.getYear() == 2010) {
				localUnitBalance = 0;
				unitBalance = 0;
				transitionFound = true;
			}

			if ((q.getMinSecs() != 0)
					&& (e.getTrnDt().getTime() < q.getMinSecs()))
				continue;
			if ((q.getMaxSecs() != 0)
					&& (e.getTrnDt().getTime() > q.getMaxSecs()))
				continue;
			// TODO: should we set balances for all accounts earlier on?
			DirectedEventRow l = funnel.from(e);
			l.setLocalUnitBalance(localUnitBalance);
			l.setBalance(unitBalance);
			rows.add(l);
		}

		EventStatistics stats = new EventStatistics();
		stats.setAccount(q.getSingleId());

		if (multiUnit) {
			stats.setUseUnit(false);
			transactions.setMultiUnit(true);
		}
		// PWG TODO
		// for (LedgerPairDBEntry e : entries) {
		// stats.updateFromEntry(e, false);
		// }
		prevStatistics = stats;

		rows = deDupeRows(rows);
		// MFM The below BREAKs column sorting
		// Collections.sort(rows);
		// fixAccountGroups(rows, q);
		// updateBalances(allRows);

		return rows;
	}

	/**
	 * Process query transfers among the selected accounts.
	 * 
	 * @param q
	 *            Query object
	 * @param transactions
	 *            SingleSidedEvents object
	 * @return List of TransferRows matching the query
	 * @throws Exception
	 */
	private List<DirectedEventRow> processIntersections(String account,
			DirectedEvents transactions) throws Exception {

		List<EnronTransactionPair100> entries;
		List<DirectedEventRow> rows;

		logger.debug("Starting process transfer intersections with query "
				+ account);

		// We now get all the transactions if it's a single account, so that we
		// can get
		// the statistics
		EventQuery q = new EventQuery();
		q.setIntersectionOnly(true);
		String[] accountList = account.split(",");
		for (String ac : accountList) {
			q.addId(ac);
		}

		entries = transferDAO.findByQuery(q);
		rows = new ArrayList<DirectedEventRow>();

		for (EnronTransactionPair100 e : entries) {
			rows.add(funnel.from(e));
		}

		rows = deDupeRows(rows);
		// MFM The below BREAKs column sorting
		// Collections.sort(rows);

		return rows;
	}

	private List<DirectedEventRow> processMultiAccount(EventQuery q)
			throws Exception {
		List<EnronTransactionPair100> entries;
		List<DirectedEventRow> rows;
		// If we have a new query, perform it. Else we look at the previous full
		// results;

		logger.debug("Starting getTransactions with query " + q.toString());

		entries = transferDAO.findByQuery(q);
		rows = new ArrayList<DirectedEventRow>();
		for (EnronTransactionPair100 e : entries) {
			rows.add(funnel.from(e));
		}
		rows = deDupeRows(rows);
		// MFM The below BREAKs column sorting
		// Collections.sort(rows);
		fixAccountGroups(rows, q);
		prevStatistics = new EventStatistics();
		return rows;

	}

	private void fixAccountGroups(List<DirectedEventRow> rows, EventQuery q) {
		// TODO Auto-generated method stub

	}

	// XXX: GET RID OF THIS
	private List<DirectedEventRow> slice(List<DirectedEventRow> rows,
			long start, long limit) {
		logger.debug("Slicing from total: " + rows.size() + " start: " + start
				+ " limit: " + limit);
		List<DirectedEventRow> newRows = new ArrayList<DirectedEventRow>();
		if (start == 0 && limit == 0) {
			return rows;
		}
		if (start < 0) { // Seems to be an ExtJS bug
			logger.debug("Trying to slice from negative start point");
			return newRows;
		}
		int offset = 0;
		int count = 0;

		for (DirectedEventRow r : rows) {
			if (offset < start) {
				++offset;
				continue;
			}
			if (count >= limit)
				break;
			newRows.add(r);
			++count;
			++offset;
		}
		logger.debug("Returning " + newRows.size() + " rows from slice");
		return newRows;
		// return rows.subList(start, limit);

	}

	/**
	 * Removes duplicates
	 * 
	 * @param rows
	 * @return
	 */
	private List<DirectedEventRow> deDupeRows(List<DirectedEventRow> rows) {
		List<DirectedEventRow> newRows = new ArrayList<DirectedEventRow>();
		// Set<TransferRow> set = new HashSet<TransferRow>();
		// set.addAll(rows);
		// newRows.addAll(set);

		// MFM The input data is already sorted
		for (DirectedEventRow rec : rows) {
			if (!newRows.contains(rec)) {
				newRows.add(rec);
			}
		}
		return newRows;
	}

	public EventStatistics getPairMonthlyStatistics(
			@QueryParam("accountNumber") String account) {
		if (!prevQuery.isSingleId()) {
			return new EventStatistics(); // to return an empty structure
		}
		return prevStatistics; // generated when previous query was executed
	}

	public EventStatistics getPairDailyStatistics(
			@QueryParam("accountNumber") String account,
			@QueryParam("year") int year, @QueryParam("month") int month)

	{
		if (!prevQuery.isSingleId()) {
			return new EventStatistics(); // to return an empty structure
		}
		if (!prevStatistics.account.equals(account)) {
			logger.debug("Account does not match the last one loaded");
			return new EventStatistics(); // to return an empty structure
		}

		EventStatistics stats = new EventStatistics();
		/*
		 * PWG TODO if support stats with pairs for (LedgerPairRow e :
		 * prevAllRows) { if (e.getYear() != year) continue; if
		 * (e.getMonth_zero_based() != month) continue; int day =
		 * e.getDay_one_based(); stats.updateFromEntry(e, true); }
		 */
		return stats;
	}

}
