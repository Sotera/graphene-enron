package graphene.enron.dao.impl;

import graphene.dao.EntityRefDAO;
import graphene.dao.IdTypeDAO;
import graphene.dao.sql.GenericDAOJDBCImpl;
import graphene.enron.model.BasicEntityRefFunnel;
import graphene.enron.model.sql.enron.EnronEntityref100;
import graphene.enron.model.sql.enron.EnronIdentifierType100;
import graphene.enron.model.sql.enron.QEnronEntityref100;
import graphene.model.idl.G_CanonicalPropertyType;
import graphene.model.idl.G_SearchTuple;
import graphene.model.idl.G_SearchType;
import graphene.model.memorydb.IMemoryDB;
import graphene.model.memorydb.MemRow;
import graphene.model.query.AdvancedSearch;
import graphene.model.query.EntityQuery;
import graphene.model.query.StringQuery;
import graphene.model.view.entities.BasicEntityRef;
import graphene.util.ExceptionUtil;
import graphene.util.FastNumberUtils;
import graphene.util.G_CallBack;
import graphene.util.validator.ValidationUtils;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.tapestry5.ioc.annotations.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.Tuple;
import com.mysema.query.sql.HSQLDBTemplates;
import com.mysema.query.sql.SQLQuery;
import com.mysema.query.sql.SQLTemplates;
import com.mysema.query.types.EntityPath;
import com.mysema.query.types.expr.BooleanExpression;
import com.mysema.query.types.path.StringPath;

public class EntityRefDAOImpl extends
		GenericDAOJDBCImpl<EnronEntityref100, EntityQuery> implements
		EntityRefDAO<EnronEntityref100, EntityQuery> {
	private static final long INITIAL_CHUNK_SIZE = 500000;
	static Logger logger = LoggerFactory.getLogger(EntityRefDAOImpl.class);

	private static final long MAX_CHUNK_SIZE = 1000000;

	// not final
	private static long MAX_RETURNABLE_RESULTS = 50000000;

	private static final long MIN_CHUNK_SIZE = 100000;
	private BasicEntityRefFunnel funnel = new BasicEntityRefFunnel();
	@Inject
	private IdTypeDAO<EnronIdentifierType100, StringQuery> idTypeDAO;
	@Inject
	private IMemoryDB memDb;

	public EntityRefDAOImpl() {

	}

	private SQLQuery buildQuery(EntityQuery q, QEnronEntityref100 t,
			Connection conn) throws Exception {
		BooleanBuilder builder = new BooleanBuilder();
		// make sure we have a list worth writing a query about.
		if (ValidationUtils.isValid(q)
				&& ValidationUtils.isValid(q.getAttributeList())) {
			ArrayList<String> optimizedIdentifierList = new ArrayList<String>(4);
			ArrayList<String> optimizedAccountNumberList = new ArrayList<String>(
					4);
			ArrayList<String> optimizedCustomerNumberList = new ArrayList<String>(
					4);
			for (G_SearchTuple<String> tuple : q.getAttributeList()) {
				// Build a boolean clause for this loop, and then 'or' it with
				// previous clauses
				/*
				 * FIXME: For repetitive clauses that don't make use of specific
				 * types, this results in some ugly queries: c=1 or c=2 or
				 * c=3...or c=100 etc. These queries may even get large enough
				 * to break the SQL statement max length.
				 * 
				 * For certain searches, where the search type is 'eq', we could
				 * combine them in to an 'in' clause which is what we used to
				 * do. e.g. c in (1,2,3,4,...100) Fixed.
				 * 
				 * FIXME: the validity of the tuple value should be checked at
				 * the beginning of each loop, before doing anything. Fixed.
				 * 
				 * FIXME: only add loopbuilder to builder when you are sure it
				 * has a value. For cases where we are doing an optimization, it
				 * WILL NOT have a value, and that causes an error later on when
				 * it is parsed. Fixed.
				 */

				if (ValidationUtils.isValid(tuple.getValue())) {
					BooleanBuilder loopBuilder = new BooleanBuilder();

					// All of the following should be exclusive.
					if (ValidationUtils
							.isValid(tuple.getSpecificPropertyType())) {
						/*
						 * A specific id type was specified, which is finer
						 * grained than the canonical enum. So we disregard any
						 * canonical enum specified and just shoot for the
						 * specific type.
						 */
						BooleanExpression b = handleSearchType(t.identifier,
								tuple);
						if (b != null) {
							loopBuilder.and(b);
						}
						loopBuilder.and(t.idtypeId.eq(FastNumberUtils
								.parseIntWithCheck(tuple
										.getSpecificPropertyType())));

					} else if (!tuple.getFamily().equals(
							G_CanonicalPropertyType.ANY)) {
						/*
						 * A family of ids (a Canonical Property Type) was
						 * specified (not ANY), which is one of the the
						 * canonical enum values. Note we have to look up the
						 * list of specific idTypes that are in this family.
						 */
						if (tuple.getFamily().equals(
								G_CanonicalPropertyType.ACCOUNT)) {
							/*
							 * Search just the accountNumber column
							 */
							if (tuple.getSearchType().equals(
									G_SearchType.COMPARE_EQUALS)) {
								// we can optimize!
								optimizedAccountNumberList
										.add(tuple.getValue());
							} else {
								BooleanExpression b = handleSearchType(
										t.accountnumber, tuple);
								if (b != null) {
									loopBuilder.and(b);
								}
							}
							/*
							 * Since we're not searching against the identifiers
							 * table, we don't filter on id types.
							 */
						} else if (tuple.getFamily().equals(
								G_CanonicalPropertyType.CUSTOMER_NUMBER)) {
							/*
							 * Search just the customerNumber column
							 */
							if (tuple.getSearchType().equals(
									G_SearchType.COMPARE_EQUALS)) {
								// we can optimize!
								optimizedCustomerNumberList.add(tuple
										.getValue());
							} else {
								BooleanExpression b = handleSearchType(
										t.customernumber, tuple);
								if (b != null) {
									loopBuilder.and(b);
								}
							}
							/*
							 * Since we're not searching against the identifiers
							 * table, we don't filter on id types.
							 */
						} else {
							/*
							 * it was some other canonical type that we don't
							 * have a specific column for, so just search all
							 * identifiers.
							 */
							loopBuilder.and(handleSearchType(t.identifier,
									tuple));
							/*
							 * But since we're searching against all
							 * identifiers, let's narrow it down by only looking
							 * at the id types associated with the canonical
							 * type.
							 */
							Integer[] idtypes = idTypeDAO
									.getTypesForFamily(tuple.getFamily());
							if (idtypes != null && idtypes.length > 0) {
								loopBuilder.and(t.idtypeId.in(idtypes));
							}

						}

					} else if (tuple.getFamily().equals(
							G_CanonicalPropertyType.ANY)) {

						if (tuple.getSearchType().equals(
								G_SearchType.COMPARE_EQUALS)) {
							// we can optimize here.
							optimizedIdentifierList.add(tuple.getValue());
						} else {
							// we can't use the c in (1,2,3...) optimization for
							// anything but eq
							loopBuilder.and(handleSearchType(t.identifier,
									tuple));

						}
					}
					// End of loop, add to main builder.
					if (loopBuilder.hasValue()) {
						builder.or(loopBuilder);
					}
				}// end valid tuple
			}// loop complete

			/*
			 * Add in optimized lists
			 */

			if (!optimizedAccountNumberList.isEmpty()) {
				builder.or(t.accountnumber.in(optimizedAccountNumberList));
			}
			if (!optimizedCustomerNumberList.isEmpty()) {
				builder.or(t.customernumber.in(optimizedCustomerNumberList));
			}
			if (!optimizedIdentifierList.isEmpty()) {
				builder.or(t.identifier.in(optimizedIdentifierList));
			}
		}

		return from(conn, t).where(builder).orderBy(t.entityrefId.asc());
	}
	@Override
	public long count(EntityQuery q) throws Exception {
		Connection conn;
		conn = getConnection();
		QEnronEntityref100 t = new QEnronEntityref100("t");
		SQLQuery sq = buildQuery(q, t, conn);
		long count = sq.count();
		conn.close();
		return count;
	}

	@Override
	public long countEdges(String id) throws Exception {
		if (memDb != null && memDb.isLoaded()) {
			long count = 0;
			count += memDb.getRowsForAccount(id).size();
			count += memDb.getRowsForCustomer(id).size();
			count += memDb.getRowsForIdentifier(id).size();
			return count;
		} else {
			Connection conn = getConnection();
			QEnronEntityref100 t = new QEnronEntityref100("t");
			long count = from(conn, t)
					.where(t.identifier.eq(id).or(t.accountnumber.eq(id))
							.or(t.customernumber.eq(id))).distinct().count();
			conn.close();
			return count;
		}
	}

	@Override
	public Set<String> entityIDsByAdvancedSearch(AdvancedSearch srch) {
		if (memDb != null && memDb.isLoaded()) {
			return memDb.entityIDsByAdvancedSearch(srch);
		}
		return null;
	}

	@Override
	public List<EnronEntityref100> findByQuery(/* long offset, long maxResults, */
	EntityQuery q) throws Exception {

		if (memDb != null && memDb.isLoaded()) {
			List<G_SearchTuple<String>> values = q.getAttributeList();
			Set<MemRow> results = new HashSet<MemRow>();
			for (G_SearchTuple<String> est : values) {
				G_CanonicalPropertyType family = est.getFamily();
				String value = est.getValue();
				if (family.equals(G_CanonicalPropertyType.ACCOUNT)) {
					logger.debug("finding account types that match " + values);
					results.addAll(memDb.getRowsForAccount(value));
				} else if (family.equals(
						G_CanonicalPropertyType.CUSTOMER_NUMBER)) {
					logger.debug("finding customer number types that match "
							+ values);
					results.addAll(memDb.getRowsForCustomer(est.getValue()));
				} else if (family.equals(G_CanonicalPropertyType.ANY)) {
					logger.debug("finding any types that match " + values);
					results.addAll(memDb.getRowsForIdentifier(value, family.getValueString()));
					results.addAll(memDb.getRowsForAccount(value));
					results.addAll(memDb.getRowsForCustomer(value));
				} else {
					logger.debug("finding identifier types that match "
							+ values);
					// just identifiers --djue
					results.addAll(memDb.getRowsForIdentifier(est.getValue(), est
							.getFamily().getValueString()));
				}

			}
			logger.debug("Found results: " + results);
			return memRowsToDBentries(results);
		} else {
			Connection conn;
			conn = getConnection();
			QEnronEntityref100 t = new QEnronEntityref100("t");
			SQLQuery sq = buildQuery(q, t, conn);
			sq = setOffsetAndLimit(q, sq);
			List<EnronEntityref100> results = sq.list(t);
			conn.close();

			return results;
		}
	}

	/**
	 * TODO: Place dialect selection at Module level, to be injected?
	 */
	@Override
	protected SQLQuery from(Connection conn, EntityPath<?>... o) {
		SQLTemplates dialect = new HSQLDBTemplates(); // SQL-dialect
		return new SQLQuery(conn, dialect).from(o);
	}

	@Override
	public Set<String> getAccountsForCustomer(String cust) throws Exception {
		if (memDb != null && memDb.isLoaded()) {
			Set<String> results = new HashSet<String>();
			Set<MemRow> rows = memDb.getRowsForCustomer(cust);
			for (MemRow r : rows) {
				results.add(memDb
						.getAccountNumberForID(r.entries[IMemoryDB.ACCOUNT]));
			}
			return results;
		}
		Connection conn;
		conn = getConnection();
		QEnronEntityref100 t = new QEnronEntityref100("t");
		List<String> rows = from(conn, t)
				.where(t.customernumber.like("%" + cust + "%")).distinct()
				.list(t.customernumber);
		conn.close();

		Set<String> results = new HashSet<String>();
		for (String e : rows)
			results.add(e);

		return results;
	}

	@Override
	public List<EnronEntityref100> getAll(long offset, long limit)
			throws Exception {
		List<Tuple> tupleResults = getAllTuples(offset, limit);
		List<EnronEntityref100> results = new ArrayList<EnronEntityref100>(
				tupleResults.size());
		// Funnel the tuple into the bean, but unset properties will stay null
		// and not take up space.
		QEnronEntityref100 t = new QEnronEntityref100("t");
		for (Tuple tuple : tupleResults) {
			EnronEntityref100 k = new EnronEntityref100();
			k.setAccountnumber(tuple.get(t.accountnumber));
			k.setCustomernumber(tuple.get(t.customernumber));
			k.setIdentifier(tuple.get(t.identifier));
			k.setIdtypeId(tuple.get(t.idtypeId));
			results.add(k);
		}
		return results;
	}

	/**
	 * Uses the full bean, but optimized SQL
	 * 
	 * @param offset
	 * @param limit
	 * @return
	 * @throws Exception
	 */
	private List<EnronEntityref100> getAllFast(long offset, long limit)
			throws Exception {
		Connection conn = getConnection();
		QEnronEntityref100 t = new QEnronEntityref100("t");
		SQLQuery sq = from(conn, t);
		/*
		 * Use the built-in Id to grab a range. Here we are assuming something
		 * about the IDs, so be careful, this code is not portable.
		 * 
		 * We also skip over some list of idTypes, again this is very
		 * customer/data specific.
		 */
		sq.where(t.entityrefId.between(offset, limit).and(
				t.idtypeId.notIn(idTypeDAO.getSkipTypes())));

		/*
		 * Note that we are not sorting the elements here, in order to speed
		 * things up.
		 */
		List<EnronEntityref100> results = sq.list(t);
		conn.close();

		return results;
	}

	/**
	 * This is the portable version of the code, kept here for posterity.
	 * 
	 * @param offset
	 * @param limit
	 * @return
	 * @throws Exception
	 */

	private List<EnronEntityref100> getAllPortable(long offset, long limit)
			throws Exception {
		Connection conn = getConnection();
		QEnronEntityref100 t = new QEnronEntityref100("t");
		SQLQuery sq = from(conn, t);
		sq = setOffsetAndLimit(offset, limit, sq);
		List<EnronEntityref100> results = sq.orderBy(t.entityrefId.asc()).list(
				t);
		conn.close();

		return results;
	}

	// A tuple version that is lighter weight -- we only ask for the columns we
	// really want. Also uses optimized SQL.
	private List<Tuple> getAllTuples(long offset, long limit) throws Exception {
		Connection conn = getConnection();
		QEnronEntityref100 t = new QEnronEntityref100("t");
		SQLQuery sq = from(conn, t);
		/*
		 * Use the built-in Id to grab a range. Here we are assuming something
		 * about the IDs, so be careful, this code is not portable.
		 * 
		 * We also skip over some list of idTypes, again this is very
		 * customer/data specific.
		 */
		sq = sq.where(t.entityrefId.goe(offset).and(t.entityrefId.lt(limit)));

		List<Integer> st = idTypeDAO.getSkipTypes();
		if (st != null && st.size() > 0) {
			logger.debug("Adding constraint to filter skipped types");
			sq.where(t.idtypeId.notIn(st));
		} else {
			logger.debug("No types to skip were specified.");
		}
		/*
		 * Note that we are not sorting the elements here, in order to speed
		 * things up.
		 */
		List<Tuple> results = sq.orderBy(t.customernumber.asc()).list(
				t.accountnumber, t.customernumber, t.identifier, t.idtypeId);
		conn.close();

		return results;
	}

	@Override
	public Set<BasicEntityRef> getBasicRowsForCustomer(String id) {
		Set<BasicEntityRef> list = new HashSet<BasicEntityRef>(3);
		try {
			for (EnronEntityref100 x : getRowsForCustomer(id)) {
				list.add(funnel.from(x));
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return list;

	}

	/**
	 * Since the table uses a numeric, non contiguous id as a primary key (even
	 * if not formally defined as one), it helps to know the maximum value it
	 * holds for when we need to iterate over all ids.
	 * 
	 * @return
	 * @throws Exception
	 */
	protected long getMaxIndexValue() throws Exception {
		Connection conn;
		conn = getConnection();
		QEnronEntityref100 t = new QEnronEntityref100("t");
		List<Integer> value = from(conn, t).list(t.entityrefId.max());

		conn.close();
		return value.get(0);
	}

	@Override
	public Set<EnronEntityref100> getRowsForCustomer(String cust)
			throws Exception {

		Set<EnronEntityref100> results = new HashSet<EnronEntityref100>();
		if (memDb != null && memDb.isLoaded()) {
			// XXX: See if this step was necessary, or if it was just Eclipse
			Set<MemRow> rows = memDb.getRowsForCustomer(cust);
			for (MemRow r : rows) {
				results.add(memRowToDBEntry(r));
			}
		} else {
			EntityQuery q = new EntityQuery();

			G_SearchTuple<String> srch = new G_SearchTuple<String>();
			srch.setSearchType(G_SearchType.COMPARE_EQUALS);
			srch.setSpecificPropertyType("customerNumber");
			srch.setValue(cust);

			List<G_SearchTuple<String>> attrs = new ArrayList<G_SearchTuple<String>>();
			attrs.add(srch);
			q.setAttributeList(attrs);
			List<EnronEntityref100> rows = findByQuery(q);
			for (EnronEntityref100 e : rows) {
				results.add(e);
			}
		}

		return results;
	}

	/**
	 * @param path
	 * @param builder
	 * @param tuple
	 * @return
	 */
	private BooleanExpression handleSearchType(StringPath path,
			G_SearchTuple<String> tuple) {
		BooleanExpression b = null;
		switch (tuple.getSearchType()) {
		case COMPARE_CONTAINS:
			b = path.contains(tuple.getValue());
			break;
		case COMPARE_STARTSWITH:
			b = path.startsWith(tuple.getValue());
			break;
		case COMPARE_ENDSWITH:
			b = path.endsWith(tuple.getValue());
			break;
		case COMPARE_EQUALS:
			b = path.eq(tuple.getValue());
			break;
		case COMPARE_REGEX:
			b = path.matches(tuple.getValue());
			break;
		case COMPARE_SIMPLE:
			b = path.like("%" + tuple.getValue() + "%");
			break;
		default:
			b = path.contains(tuple.getValue());
			break;
		}
		if (b == null) {
			logger.error("Could not make a boolean expression for search type "
					+ tuple.getSearchType());
		}
		return b;
	}

	@Override
	public boolean isReady() {
		boolean ready = false;
		if (memDb != null && memDb.isLoaded()) {
			// wait until memdb is loaded.
			ready = true;
		} else {
			// using sql backend
			ready = super.isReady();
		}
		return ready;
	}

	private List<EnronEntityref100> memRowsToDBentries(Set<MemRow> ms) {
		List<EnronEntityref100> results = new ArrayList<EnronEntityref100>();
		for (MemRow m : ms) {
			results.add(memRowToDBEntry(m));
		}
		//logger.debug("Converted results: " + results);
		return results;
	}

	private EnronEntityref100 memRowToDBEntry(MemRow m) {
		EnronEntityref100 pb = new EnronEntityref100();
		pb.setAccountnumber(memDb
				.getAccountNumberForID(m.entries[IMemoryDB.ACCOUNT]));
		pb.setCustomernumber(memDb
				.getCustomerNumberForID(m.entries[IMemoryDB.CUSTOMER]));//
		pb.setIdtypeId(m.getIdType());
		pb.setIdentifier(memDb.getIdValueForID(m.entries[IMemoryDB.IDENTIFIER]));
		pb.setEntityrefId(m.offset); // used for deduplication
		return pb;
	}

	@Override
	public boolean performCallback(long offset, long maxResults,
			G_CallBack<EnronEntityref100> cb, EntityQuery q) {

		long MAX_VALUE = 100000000;
		try {
			MAX_VALUE = getMaxIndexValue();
		} catch (Exception e) {
			logger.error("Could not retrieve max index value: "
					+ ExceptionUtil.getRootCauseMessage(e));
		}
		return throttlingCallbackOnValues(offset, maxResults, cb, q,
				INITIAL_CHUNK_SIZE, MIN_CHUNK_SIZE, MAX_CHUNK_SIZE, 0,
				MAX_VALUE);
	}

	@Override
	public Set<String> regexSearch(String name, String family,
			boolean caseSensitive) {
		if (memDb != null && memDb.isLoaded()) {
			Set<String> matches = memDb.findRegexMatches(name,

			family, caseSensitive);
			return matches;
		} else {
			// not implemented
			return null;
		}
	}

	@Override
	public List<EnronEntityref100> rowSearch(EntityQuery q) throws Exception {
		if (memDb != null && memDb.isLoaded()) {
			List<G_SearchTuple<String>> values = q.getAttributeList();
			Set<MemRow> results = new HashSet<MemRow>();
			for (G_SearchTuple<String> s : values) {
				if (s.getFamily().equals(G_CanonicalPropertyType.ACCOUNT)) {
					results.addAll(memDb.getRowsForAccount(s.getValue()));
				} else if (s.getFamily().equals(
						G_CanonicalPropertyType.CUSTOMER_NUMBER)) {
					results.addAll(memDb.getRowsForCustomer(s.getValue()));
				} else {
					// all other families
					results.addAll(memDb.getRowsForIdentifier(s.getValue(), s
							.getFamily().getValueString()));
				}

			}
			return memRowsToDBentries(results);
		} else {
			Connection conn;
			conn = getConnection();
			QEnronEntityref100 t = new QEnronEntityref100("t");
			SQLQuery sq = buildQuery(q, t, conn);
			sq = setOffsetAndLimit(q.getFirstResult(), q.getMaxResult(), sq);

			List<EnronEntityref100> results = sq.list(t);
			conn.close();

			return results;
		}
	}

	@Override
	public Set<String> soundsLikeSearch(String src, String family) {
		if (memDb != null && memDb.isLoaded()) {
			Set<String> matches = memDb.findSoundsLikeMatches(src, family);
			return matches;
		}
		return null;
	}

	@Override
	public Set<String> valueSearch(EntityQuery q) throws Exception {
		if (memDb != null && memDb.isLoaded()) {

			/**
			 * This is a kludge. We should pass the entire tuple list to the
			 * 'DAO' for processing. XXX: Fix this.
			 */
			Set<String> stringList = new HashSet<String>();
			for (G_SearchTuple<String> tuple : q.getAttributeList()) {
				stringList.add(tuple.getValue());
			}

			Set<String> values = memDb.getValuesContaining(stringList,
					q.isCaseSensitive());

			// Not yet filtered by family
			Set<String> results = new HashSet<String>();
			// XXX: Super kludge
			G_CanonicalPropertyType family = q.getAttributeList().get(0)
					.getFamily();
			// String family = q.getIdFamily();

			for (String v : values) {
				if (memDb.isIdFamily(v, family.getValueString()))
					results.add(v);
			}
			return results;
		}
		Set<String> results = new HashSet<String>();

		// Note the original version of this used 'like' the comparison by
		// default.
		Connection conn;
		conn = getConnection();
		QEnronEntityref100 t = new QEnronEntityref100("t");
		SQLQuery sq = buildQuery(q, t, conn);
		sq = setOffsetAndLimit(q.getFirstResult(), q.getMaxResult(), sq);
		results.addAll(sq.distinct().list(t.identifier));
		conn.close();

		return results;
	}
}
