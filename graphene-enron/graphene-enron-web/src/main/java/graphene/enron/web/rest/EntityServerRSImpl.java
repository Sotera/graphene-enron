package graphene.enron.web.rest;

import graphene.dao.DataSourceListDAO;
import graphene.dao.EntityDAO;
import graphene.model.query.AdvancedSearch;
import graphene.model.view.entities.Entity;
import graphene.model.view.entities.EntityLight;
import graphene.model.view.entities.EntitySearchResults;
import graphene.rest.ws.EntityServerRS;
import graphene.util.stats.TimeReporter;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.apache.tapestry5.ioc.annotations.Inject;
import org.slf4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;


public class EntityServerRSImpl implements EntityServerRS {

	@Inject
	private Logger logger;// = LoggerFactory.getLogger(SearchServerRS.class);
	
	@Inject
	private DataSourceListDAO dataSourceListDAO;
	
	@Inject
	private EntityDAO entitydao;

	@Override
	@GET
	@Path("/advancedSearch")
	@Produces("application/json")
	public EntitySearchResults advancedSearch(
			@QueryParam("jsonSearch") String jsonSearch) {
		TimeReporter t = new TimeReporter("advancedSearch", logger);
		logger.trace("json search: " + jsonSearch);
		ObjectMapper mapper = new ObjectMapper();
		byte[] bytes = jsonSearch.getBytes();
		AdvancedSearch search = null;
		try {
			search = mapper.readValue(bytes, 0, bytes.length,
					AdvancedSearch.class);
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		EntitySearchResults results = new EntitySearchResults();
		if (search == null) {
			logger.debug("json parse failed");
			results = null;
		} else {
			logger.trace(search.getDataSet());
			search.setFieldsIntoFilters(dataSourceListDAO.getList());
			List<Entity> entities = entitydao.getEntitiesByAdvancedSearch(search);
			results.addEntities(entities);
		}
		t.logAsCompleted();
		return results;
	}

	@Override
	@GET
	@Path("/getEntityByID/{ID}")
	@Produces("application/json")
	/**
	 * Returns an entity given the ID. May not be needed, since
	 * all the search responses may have complete entities contained
	 * in them. 
	 */
	public EntityLight getEntityByID(@PathParam("ID") String id) {
		logger.debug("Getting entity for id " + id);
		return new EntityLight(entitydao.getById(id));
	}

}
