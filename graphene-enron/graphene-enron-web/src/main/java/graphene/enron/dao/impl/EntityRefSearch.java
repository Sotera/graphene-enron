package graphene.enron.dao.impl;

import graphene.dao.EntityRefDAO;
import graphene.dao.IdTypeDAO;
import graphene.enron.model.sql.enron.EnronEntityref100;
import graphene.model.idl.G_CanonicalPropertyType;
import graphene.model.idl.G_Delimiter;
import graphene.model.idl.G_SearchTuple;
import graphene.model.idl.G_SearchType;
import graphene.model.query.EntityQuery;
import graphene.model.query.EntitySearchTypeHelper;
import graphene.model.view.entities.CustomerDetails;
import graphene.model.view.entities.EntitySearchResponse;
import graphene.model.view.entities.IdType;
import graphene.model.view.namesearch.NameSearchResponse;
import graphene.model.view.namesearch.NameSearchResult;
import graphene.search.ScoredResultsBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.tapestry5.ioc.annotations.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Backing service for the REST search services. <BR>
 * Searches the Entityref table which was (primarily) derived from the customer
 * records <BR>
 * Accessed by: SearchServerRSImpl <BR>
 * Accesses: EntityRefAccess interface, which is implemented by either the
 * memory DB or the SQL DB.
 * 
 * @author PWG for DARPA
 * 
 * 
 *         We need to be able to search for Entities (customers here) by family
 *         and value.
 */
public class EntityRefSearch {
	static final String _0_INVALID_SEARCH_TYPE = "0: invalid search type ";
	static final String _0_NO_MATCHES_FOUND = "0: No matches found";
	static Logger logger = LoggerFactory.getLogger(EntityRefSearch.class);

	@Inject
	private EntityRefDAO<EnronEntityref100, EntityQuery> dao;

	@Inject
	private IdTypeDAO<EnronEntityref100, EntityQuery> idTypeDao;

	private List<CustomerDetails> addRowsPerAccount(
			Collection<CustomerDetails> custs) {
		List<CustomerDetails> results = new ArrayList<CustomerDetails>();
		for (CustomerDetails c : custs) {
			if (c.getAccountSet().size() < 2)
				results.add(c);
			else {
				HashSet<String> set = new HashSet<String>();
				set.addAll(c.getAccountSet());
				for (String a : set) {
					try {
						CustomerDetails cnew = c.clone();
						cnew.setAccountSet(new HashSet<String>());
						cnew.addAccount(a);
						results.add(cnew);
					} catch (CloneNotSupportedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			}
		}
		return results;
	}

	/**
	 * Return a list of distinct customer names matching a query The query is in
	 * enhanced search format - e.g. +foo -bar <BR/>
	 * Searches the database or memory index
	 * 
	 * @param name
	 *            String the name to search for
	 * @param caseSensitive
	 * @return Set<String> names
	 */

	public Set<String> customerNameMatchEnhanced(String name,
			boolean caseSensitive) {
		Set<String> results = new HashSet<String>();
		EntityQuery q = new EntityQuery();

		List<G_SearchTuple<String>> tupleList = EntitySearchTypeHelper
				.processSearchList(name, G_SearchType.COMPARE_CONTAINS,
						G_CanonicalPropertyType.ANY);
		q.setAttributeList(tupleList);

		//
		// EntityRefQuery q = EntityRefQuery.makeFromSearchString(name, "name",
		// caseSensitive);

		Set<String> vals = null;
		try {
			vals = dao.valueSearch(q);
			logger.debug("pb.valueSearch returned " + vals.size());
			results.addAll(ScoredResultsBuilder
					.build(name, vals, caseSensitive));
			logger.debug("after post-process there are " + results.size());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return results;
	}

	/**
	 * This is called when the user has selected a customer name from a list of
	 * matches. We return all customers with that exact name, together with
	 * their details
	 * 
	 * @param name
	 *            the name to search for
	 * @return CustomerSearchResponse
	 * @throws Exception
	 **/
	public EntitySearchResponse findCustomersByExactName(String value,
			boolean rowPerAccount) throws Exception {

		EntitySearchResponse sr = new EntitySearchResponse();
		if (null == value) {
			logger.warn("values object was null, returning an empty search response");
			return sr;
		}

		EntityQuery customerNumberQuery = new EntityQuery();
		customerNumberQuery.setAttributeList(EntitySearchTypeHelper
				.processSearchList(value, G_SearchType.COMPARE_EQUALS,
						G_CanonicalPropertyType.NAME));

		List<EnronEntityref100> rows = dao.findByQuery(customerNumberQuery);

		Set<String> cnumbers = new HashSet<String>();
		for (EnronEntityref100 r : rows) {
			cnumbers.add(r.getCustomernumber());
		}
		logger.debug("Found " + cnumbers.size() + " customers with name "
				+ value);

		EntityQuery customerDetailsQuery = new EntityQuery();
		for (String cno : cnumbers) {
			G_SearchTuple<String> e = new G_SearchTuple<String>(
					G_SearchType.COMPARE_EQUALS,
					G_CanonicalPropertyType.CUSTOMER_NUMBER, null, cno);
			customerDetailsQuery.addAttribute(e);
		}

		List<EnronEntityref100> crows = dao.findByQuery(customerDetailsQuery);

		for (CustomerDetails cd : rowsToCustomers(crows)) {
			sr.getResults().add(cd);
		}

		/*
		 * FIXME: this seems unnecessary, since we would have encountered all
		 * the accounts in the previous query.
		 */
		for (CustomerDetails c : sr.getResults()) {
			Set<String> accounts = dao.getAccountsForCustomer(c
					.getCustomerNumber());
			for (String ac : accounts) {
				c.addAccount(ac);
			}
		}

		if (rowPerAccount) {
			sr.setResults(addRowsPerAccount(sr.getResults()));
		}

		if (sr.getResults() != null)
			logger.info("Found " + sr.getResults().size() + " customers");

		return sr;
	}

	/*
	 * public static List<EnronEntityref100>
	 * checkForCase(List<EnronEntityref100> results, String value, int findType)
	 * { List<EnronEntityref100> matching = new ArrayList<EnronEntityref100>();
	 * for (EnronEntityref100 pb : results) { switch (findType) { case
	 * Config.COMPARE_EQUALS: if (pb.getIdValue().equals(value))
	 * matching.add(pb); break; case Config.COMPARE_STARTSWITH: if
	 * (pb.getIdValue().startsWith(value)) matching.add(pb); break; case
	 * Config.COMPARE_CONTAINS: if (pb.getIdValue().contains(value))
	 * matching.add(pb); break; default: break; }
	 * 
	 * }
	 * 
	 * return matching; }
	 */
	/**
	 * Note: the old version used spaces as delimiters.
	 * 
	 * @see G_Delimiter
	 * @param value
	 *            a delimited string
	 * @param maxResults
	 * @param b
	 * @return
	 * @throws Exception
	 */
	public EntitySearchResponse findCustomersForAccounts(String value,
			int maxResults, boolean b) throws Exception {
		// TODO Auto-generated method stub
		EntitySearchResponse sr = new EntitySearchResponse();
		if (null == value) {
			logger.warn("values object was null, returning an empty search response");
			return sr;
		}

		EntityQuery customerQuery = new EntityQuery();
		customerQuery.setMaxResult(maxResults);
		customerQuery.setAttributeList(EntitySearchTypeHelper
				.processSearchList(value, G_SearchType.COMPARE_EQUALS,
						G_CanonicalPropertyType.ACCOUNT));

		List<EnronEntityref100> rows = dao.findByQuery(customerQuery);

		for (CustomerDetails cd : rowsToCustomers(rows)) {
			sr.getResults().add(cd);
		}

		for (CustomerDetails c : sr.getResults()) {
			Set<String> accounts = dao.getAccountsForCustomer(c
					.getCustomerNumber());
			for (String ac : accounts) {
				c.addAccount(ac);
			}
		}

		if (sr.getResults() != null) {
			logger.info("Found " + sr.getResults().size() + " customers");
		}

		return sr;
	}

	/**
	 * 
	 * This is called from the web services to do the initial search (not
	 * traversals).
	 * 
	 * The initial search can be by name, address, account, or any. It can be
	 * simple, regex or soundex
	 * 
	 * @param searchType
	 *            String: 'simple', 'equals', 'regex', 'soundex'
	 * @param value
	 *            String: value to search for. Enhanced search string if type is
	 *            simple
	 * @param family
	 *            String identifier type family
	 * @param maxResults
	 *            maximum number of results to return
	 * @param caseSensitive
	 * @param rowPerAccount
	 * @return
	 * @throws Exception
	 */
	public EntitySearchResponse findCustomersForIdentifier(String searchType,
			String value, String family, int maxResults, boolean caseSensitive,
			boolean rowPerAccount) throws Exception {

		EntitySearchResponse sr = new EntitySearchResponse();
		if (null == value) {
			logger.warn("values object was null, returning an empty search response");
			return sr;
		}

		EntityQuery identifierQuery = new EntityQuery();
		// Note: we use compare contains as the default because the original
		// implementation of valueSearch(), used below, had a like operator. You
		// have the option here of being more precise if you construct the query
		// string properly.
		identifierQuery.setAttributeList(EntitySearchTypeHelper
				.processSearchList(value, G_SearchType.fromValue(searchType),
						G_CanonicalPropertyType.fromValue(family)));
		identifierQuery.setFirstResult(0);
		identifierQuery.setMaxResult(maxResults);
		identifierQuery.setCaseSensitive(caseSensitive);

		logger.trace("Search Type:'" + searchType + "' " + "Value: '" + value
				+ "' family: " + family + " Max Results: " + maxResults);

		Set<String> identifierValues = dao.valueSearch(identifierQuery);
		// Something like Bob,bob,mabob,joebob

		logger.debug("Found " + identifierValues.size() + " matches on "
				+ value);
		// values is now a set of unique identifier values matching the search
		// Now we find customers for those values
		// TODO: Do we use the same search type here? This is not a very
		// efficient way of doing joins.
		EntityQuery customerQuery = new EntityQuery();
		for (String identifier : identifierValues) {
			customerQuery.addAttribute(
					new G_SearchTuple<>(G_SearchType.fromValue(searchType),
							G_CanonicalPropertyType.ANY, null, identifier));
		}
		List<EnronEntityref100> rows = dao.findByQuery(customerQuery);

		for (CustomerDetails cd : rowsToCustomers(rows)) {
			sr.getResults().add(cd);
		}

		for (CustomerDetails c : sr.getResults()) {
			Set<String> accounts = dao.getAccountsForCustomer(c
					.getCustomerNumber());
			for (String ac : accounts) {
				c.addAccount(ac);
			}
		}

		if (rowPerAccount) {
			sr.setResults(addRowsPerAccount(sr.getResults()));
		}

		if (sr.getResults() != null) {
			logger.info("Found " + sr.getResults().size() + " customers");
		}

		return sr;
	}

	/**
	 * 
	 * @param searchType
	 * @param name
	 * @param maxResults
	 * @param caseSensitive
	 * @return
	 */
	public NameSearchResponse findMatchingCustomerNames(String searchType,
			String name, int maxResults, boolean caseSensitive) {

		NameSearchResponse sr = new NameSearchResponse();
		if (null == name) {
			logger.warn("values object was null, returning an empty search response");
			return sr;
		}
		Set<String> results = null;

		if (searchType.equals("simple"))
			results = customerNameMatchEnhanced(name, caseSensitive);
		else if (searchType.equals("regex"))
			results = dao.regexSearch(name, "name", caseSensitive);
		else if (searchType.equals("soundslike"))
			results = dao.soundsLikeSearch(name, "name");
		else {
			logger.error("Invalid search type " + searchType);
			results = new HashSet<String>();
		}
		if (results == null) {
			logger.error("Unsupported search type");
			results = new HashSet<String>();
		}

		for (String s : results)
			sr.addResult(new NameSearchResult(s, 0, 0));
		// TODO: Include the score and index in the results
		sr.sortResults();
		sr.setCount(sr.getResults().size());

		return sr;
	}

	private Collection<CustomerDetails> rowsToCustomers(
			List<EnronEntityref100> entries) {
		Map<String, CustomerDetails> custs = new HashMap<String, CustomerDetails>();
		CustomerDetails c;
		for (EnronEntityref100 e : entries) {
			String cno = e.getCustomernumber();
			c = custs.get(cno);
			if (c == null) {
				c = new CustomerDetails(cno);
			}
			IdType id = idTypeDao.getByType(e.getIdtypeId());
			if (id == null) {
				logger.error("Could not find id number for " + e.getIdtypeId());
			}
			c.addIdentifier(id, e.getIdentifier());
			/*
			 * FIXME: Should be able to encounter all the possible accounts for
			 * this customer inside this loop
			 */
			custs.put(cno, c);
		}
		return custs.values();

	}
}
