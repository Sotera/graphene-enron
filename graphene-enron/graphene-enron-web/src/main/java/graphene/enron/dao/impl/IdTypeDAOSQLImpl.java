package graphene.enron.dao.impl;

import graphene.dao.IdTypeDAO;
import graphene.dao.sql.GenericDAOJDBCImpl;
import graphene.enron.model.sql.enron.EnronIdentifierType100;
import graphene.enron.model.sql.enron.QEnronIdentifierType100;
import graphene.model.idl.G_CanonicalPropertyType;
import graphene.model.view.entities.IdType;
import graphene.util.CallBack;
import graphene.util.validator.ValidationUtils;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tapestry5.ioc.annotations.Inject;
import org.slf4j.Logger;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.sql.SQLQuery;

/**
 * All implementations requested from the IOC registry are singletons by
 * default, therefore we don't need static members. If for some reason we needed
 * to reload the singleton service, it would refresh the values for us.
 * 
 * @author djue
 * 
 */
public class IdTypeDAOSQLImpl extends
		GenericDAOJDBCImpl<EnronIdentifierType100, String> implements
		IdTypeDAO<EnronIdentifierType100, String> {

	private boolean loaded;
	private Map<Integer, IdType> loadedTypes = new HashMap<Integer, IdType>();

	@Inject
	private Logger logger;

	private List<Integer> skipTypes = new ArrayList<Integer>();

	@Override
	public boolean applySkipRule(EnronIdentifierType100 id) {
		return false;
	}

	private SQLQuery buildQuery(String q, QEnronIdentifierType100 t,
			Connection conn) throws Exception {
		BooleanBuilder builder = new BooleanBuilder();

		if (ValidationUtils.isValid(q)) {
			builder.and(t.shortName.eq(q));
		}

		SQLQuery sq = from(conn, t).where(builder);
		return sq;
	}

	@Override
	public long count(String q) throws Exception {
		long results = 0;
		QEnronIdentifierType100 t = new QEnronIdentifierType100("t");
		Connection conn;
		conn = getConnection();
		SQLQuery sq = buildQuery(q, t, conn).orderBy(t.idtypeId.asc());
		results = sq.count();
		conn.close();

		logger.debug("Counted " + results + " entries");

		return results;
	}

	@Override
	public void createFamilyMap() {
		// TODO Auto-generated method stub

	}

	@Override
	public List<EnronIdentifierType100> findByQuery(long offset,
			long maxResults, String q) throws Exception {
		List<EnronIdentifierType100> results;
		QEnronIdentifierType100 t = new QEnronIdentifierType100("t");
		Connection conn;
		conn = getConnection();
		SQLQuery sq = buildQuery(q, t, conn).orderBy(t.idtypeId.asc());
		results = sq.list(t);
		conn.close();
		if (results != null) {
			logger.debug("Returning " + results.size() + " entries");
		}
		return results;
	}

	@Override
	public List<EnronIdentifierType100> getAll(long offset, long maxResults)
			throws Exception {

		List<EnronIdentifierType100> results;
		QEnronIdentifierType100 t = new QEnronIdentifierType100("t");
		Connection conn;
		conn = getConnection();
		SQLQuery sq = from(conn, t);
		results = sq.list(t);
		conn.close();
		if (results != null) {
			logger.debug("Returning " + results.size() + " entries");
		}
		return results;

	}

	@Override
	public IdType getByType(int typeno) {

		return getLoadedTypes().get(typeno);

	}

	@Override
	public String getColumnSource(int type) {
		if (isLoaded()) {
			String retValue = null;
			try {
				retValue = getLoadedTypes().get(type).getColumnSource();
			} catch (Exception aexp) {
				if (type == 36) {
					retValue = "Address";
				} else {
					retValue = "Other Name";
				}
			}
			return retValue;
		} else {
			return null;
		}
	}

	@Override
	public String getFamily(int type) {
		if (isLoaded()) {
			IdType id = getLoadedTypes().get(type);
			if (id == null) {
				logger.error("IdTypeCache: getFamily: could not get id definition for type "
						+ type);

				// MFM temporary fix to avoid NPEs
				if (type == 36) {
					return "address";
				} else if (type == 40) {
					return "name";
				} else {
					return "combo";
				}
				// OLD return null;
			}
			return id.getFamily();
		} else {
			return null;
		}

	}


	/**
	 * This public version causes a caching event to occur when it is first
	 * called.
	 */
	@Override
	public Map<Integer, IdType> getLoadedTypes() {
		if (loadedTypes == null || loadedTypes.isEmpty()) {
			init();
		}
		return loadedTypes;
	}

	@Override
	public String getLongName(int type) {
		return getColumnSource(type);
	}

	@Override
	public String getShortName(int type) {
		if (isLoaded()) {
			String retValue = null;
			try {
				retValue = getLoadedTypes().get(type).getShortName();
			} catch (Exception aexp) {
				if (type == 36) {
					retValue = "Address";
				} else {
					retValue = "Other Name";
				}
			}
			return retValue;
		} else {
			return null;
		}
	}

	@Override
	public List<Integer> getSkipTypes() {
		// Note, we're calling this here so that skiptypes will be initialized
		// if it was never accessed before.
		if (loadedTypes == null || loadedTypes.isEmpty()) {
			init();
		}
		return skipTypes;
	}

	@Override
	public String getTableSource(int type) {

		return getLoadedTypes().get(type).getTableSource();

	}

	// FIXME: There should be an O(1) way of doing this.
	@Override
	public int getTypeByShortName(String shortName) {
		for (IdType id : getLoadedTypes().values()) {
			if (id.getShortName().equals(shortName)) {
				return id.getIdType_id();
			}
		}
		return 0;
	}

	// FIXME: There should be an O(1) way of doing this.
	@Override
	public Integer[] getTypesForFamily(G_CanonicalPropertyType family) {
		List<Integer> results = new ArrayList<Integer>();
		for (IdType s : getLoadedTypes().values()) {
			if (s.getFamily().equalsIgnoreCase(family.getValueString()))
				results.add(Integer.valueOf(s.getIdType_id()));
		}
		return results.toArray(new Integer[results.size()]);
	}

	@Override
	public void init() {
		loadedTypes = new HashMap<Integer, IdType>(100);
		try {
			for (EnronIdentifierType100 id : getAll(0, 0)) {
				if (applySkipRule(id) == false) {

					IdType idType = new IdType();
					idType.setColumnSource(id.getColumnsource());
					idType.setFamily(id.getFamily());
					idType.setIdType_id(id.getIdtypeId());
					idType.setShortName(id.getShortName());
					idType.setTableSource(id.getTablesource());
					idType.setType(G_CanonicalPropertyType.fromValue(id
							.getFamily()));
					if (idType.getType() == null) {
						logger.warn("G_CanonicalPropertyType for "
								+ idType.toString()
								+ " was null.  This shouldn't happen");
					}
					// each idTypeId is unique.
					loadedTypes.put(id.getIdtypeId(), idType);
				}
			}
			logger.debug("Will use " + loadedTypes.size() + " Type definitions");
			logger.trace(loadedTypes.values().toString());
			loaded = true;
		} catch (Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			loaded = false;
		}
	}

	@Override
	public boolean isBadIdentifier(String id) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isLoaded() {
		return loaded;
	}

	@Override
	public boolean performCallback(long offset, long maxResults,
			CallBack<EnronIdentifierType100> cb, String q) {
		return basicCallback(offset, maxResults, cb, q);
	}

	@Override
	public void setLoaded(boolean l) {
		loaded = l;
	}

	@Override
	public void setLoadedTypes(Map<Integer, IdType> lt) {
		loadedTypes = lt;
	}

	@Override
	public void setSkipTypes(List<Integer> skipTypes) {
		this.skipTypes = skipTypes;
	}

}
