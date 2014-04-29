package graphene.enron.dao.impl;

import graphene.dao.DataSourceListDAO;
import graphene.model.datasourcedescriptors.DataSet;
import graphene.model.datasourcedescriptors.DataSetField;
import graphene.model.datasourcedescriptors.DataSource;
import graphene.model.datasourcedescriptors.DataSourceList;

/**
 * A DAO to return a list of available Data Sets. In some environments this is
 * done via a server call, so it should be an injected implementation.
 * 
 * @author PWG for DARPA
 * 
 */
public class DataSourceListDAOImpl implements DataSourceListDAO {
	private static DataSourceList dataSourceList = null;

	public DataSourceListDAOImpl() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * Return a list of datasets for use by the rest service. These lists are
	 * used by the gui to allow users to choose a list, and to configure the
	 * appropriate screens.
	 * 
	 * @author PWG for DARPA
	 * 
	 */
	public DataSourceList getList() {
		if (dataSourceList == null)
			dataSourceList = loadList();

		return dataSourceList;
	}

	private DataSourceList loadList() {
		DataSourceList list = new DataSourceList();
		// such datasource
		list.addSource(makeEnron());
		// add more data sources here if you want. wow.
		return list;
	}

	private DataSource makeEnron() {
		DataSource dataSource = new DataSource();
		DataSet ds = new DataSet();

		dataSource.setId("Enron");
		dataSource.setName("Enron");
		dataSource.setFriendlyName("Enron Email List");

		dataSource.addProperty("Country", "USA");

		ds.setName("Entities");
		ds.setEntity(true);
		ds.setTransaction(false);

		ds.addField(new DataSetField("name", "Name", "string", false, true,
				true));
		ds.addField(new DataSetField("email", "Email Address", "string", false,
				true, true));
		dataSource.addDataSet(ds);

		ds = new DataSet();
		ds.setName("Emails");
		ds.setEntity(false);
		ds.setTransaction(true);
		dataSource.addDataSet(ds);

		return dataSource;
	}

}
