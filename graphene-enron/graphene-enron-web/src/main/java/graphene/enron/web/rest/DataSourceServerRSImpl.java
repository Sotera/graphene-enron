package graphene.enron.web.rest;

import graphene.dao.DataSourceListDAO;
import graphene.model.datasourcedescriptors.DataSourceList;
import graphene.rest.ws.DataSourceServerRS;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.tapestry5.ioc.annotations.Inject;

@Path("/datasources")
public class DataSourceServerRSImpl implements DataSourceServerRS {

	// TODO:Not needed since dao is a persistent singleton.
	private static DataSourceList list = null;
	@Inject
	private DataSourceListDAO dao; // PWG TODO not being found by Tapestry IOC

	// -----------------------------------------------------------------------
	@Produces("application/json")
	@GET
	@Path("/getAll")
	public DataSourceList getAll() {
		if (list == null) {
			list = dao.getList();
		}
		return list;
	}

}