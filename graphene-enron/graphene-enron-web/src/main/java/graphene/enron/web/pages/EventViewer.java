package graphene.enron.web.pages;

import graphene.dao.TransactionDAO;
import graphene.model.idl.G_SearchTuple;
import graphene.model.idl.G_SearchType;
import graphene.model.idl.G_VisualType;
import graphene.model.query.EntityQuery;
import graphene.model.query.EventQuery;
import graphene.model.query.SearchCriteria;
import graphene.model.query.SearchTypeHelper;
import graphene.model.view.events.DirectedEventRow;
import graphene.util.ExceptionUtil;
import graphene.util.validator.ValidationUtils;
import graphene.web.annotations.PluginPage;
import graphene.web.annotations.ProtectedPage;
import graphene.web.model.DirectedEventDataSource;
import graphene.web.pages.SimpleBasePage;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.apache.tapestry5.alerts.AlertManager;
import org.apache.tapestry5.alerts.Duration;
import org.apache.tapestry5.alerts.Severity;
import org.apache.tapestry5.annotations.InjectComponent;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.annotations.SessionState;
import org.apache.tapestry5.beaneditor.BeanModel;
import org.apache.tapestry5.corelib.components.Zone;
import org.apache.tapestry5.grid.GridDataSource;
import org.apache.tapestry5.ioc.Messages;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.BeanModelSource;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.ajax.AjaxResponseRenderer;
import org.slf4j.Logger;

//@RequiresRoles("user")
@PluginPage(visualType = G_VisualType.SEARCH)
public class EventViewer extends SimpleBasePage {

	// Handle event "selected"
	private enum Mode {
		ACCOUNT, CUSTOMER;
	}

	@Inject
	private AjaxResponseRenderer ajaxResponseRenderer;
	@Inject
	private AlertManager alertManager;

	// @InjectComponent
	// private Zone result;
	@SessionState
	@Property
	private SearchCriteria criteria;
	@Persist
	@Property
	private String currentEntity;

	@Inject
	private TransactionDAO dao;

	private Mode drillDown;

	private String drillDownId;

	@Property
	private DirectedEventRow drillDownevent;

	@InjectComponent
	private Zone drillDownZone;

	@Property
	private GridDataSource gds = new DirectedEventDataSource(dao);

	@Property
	@Persist
	private boolean highlightZoneUpdates;

	@Property
	private DirectedEventRow listevent;

	// /////////////////////////////////////////////////////////////////////
	// FILTER
	// /////////////////////////////////////////////////////////////////////

	@InjectComponent
	private Zone listZone;

	@Inject
	private Logger logger;

	@Inject
	private AlertManager manager;

	@Persist
	@Property
	private String partialName;

	@Inject
	private Request request;

	@Property
	private List<DirectedEventRow> events;
	private String previousId;

	public Format getDateFormat() {
		return new SimpleDateFormat(getDatePattern());
	}

	public String getDatePattern() {
		return "dd/MM/yyyy";
	}

	public String getZoneUpdateFunction() {
		return highlightZoneUpdates ? "highlight" : "show";
	}

	public boolean isModeDrillDownAccount() {
		return drillDown == Mode.ACCOUNT;
	}

	public boolean isModeDrillDownevent() {
		return drillDown == Mode.CUSTOMER;
	}

	public boolean isModeNull() {
		return drillDown == null;
	}

	void onSelected(String customerId) {
		drillDown = Mode.ACCOUNT;
		drillDownId = customerId;

		if (request.isXHR()) {
			EntityQuery q = new EntityQuery();
			List<G_SearchTuple<String>> tupleList = SearchTypeHelper
					.processSearchList(customerId, G_SearchType.COMPARE_EQUALS);
			q.setAttributeList(tupleList);
			q.setCustomerQueryFlag(true);

			List<DirectedEventRow> list = null;
			try {
				// FIXME: Need to set limit and offset in query object
				list = dao.getEvents(q);
			} catch (Exception ex) {
				String message = ExceptionUtil.getRootCauseMessage(ex);
				manager.alert(Duration.SINGLE, Severity.ERROR, "ERROR: "
						+ message);
				logger.error(message);
			}
			if (list != null && list.size() > 0) {
				drillDownevent = list.get(0);
			}
			ajaxResponseRenderer.addRender(listZone).addRender(drillDownZone);
		}
	}

	void onSuccessFromFilterForm() {
		if (events == null || events.isEmpty()
				|| !previousId.equalsIgnoreCase(partialName)) {
			// don't use cached version.
			EventQuery q = new EventQuery();
			q.addId(partialName);
			try {
				// FIXME: Need to set limit and offset in query object
				events = dao.getEvents(q);
			} catch (Exception ex) {
				// record error to screen!
				String message = ExceptionUtil.getRootCauseMessage(ex);
				manager.alert(Duration.SINGLE, Severity.ERROR, "ERROR: "
						+ message);
				logger.error(message);
				events = new ArrayList<DirectedEventRow>();
			}
			previousId = partialName;
		}
		if (request.isXHR()) {
			logger.debug("Rendering AJAX response");
			ajaxResponseRenderer.addRender(listZone);
		}
	}

	@Inject
	private BeanModelSource beanModelSource;
	@Inject
	private Messages messages;

	public BeanModel getModel() {
		BeanModel<DirectedEventRow> model = beanModelSource.createEditModel(
				DirectedEventRow.class, messages);
		model.exclude("comments", "credit", "dateMilliSeconds",
				"day_one_based", "debit", "id", "localUnitBalance", "unit",
				"unitBalance", "year");

		// model.add("senderName",new
		// MapPropertyConduit("senderName",String.class));
		// model.add("receiverName",new
		// MapPropertyConduit("receiverName",String.class));
		model.reorder("date", "senderId", "receiverId");
		return model;
	}

	// @OnEvent(value = EventConstants.SUCCESS)
	// Object searchEntities() {
	// alertManager.info("searchEntities Happened");
	// return result.getBody();
	// }

	void setupRender() {
		// gds = new DirectedEventDataSource(dao);
		if (ValidationUtils.isValid(partialName)) {
			EventQuery e = new EventQuery();
			e.addId(partialName);
			try {
				events = dao.findByQuery(e);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} else {
			events = new ArrayList();
		}
		// Optional: Sort by the first column if no sort is set.
		// if (grid.getSortModel().getSortConstraints().isEmpty()) {
		// String firstColumn = (String) grid.getDataModel()
		// .getPropertyNames().get(0);
		// grid.getSortModel().updateSort(firstColumn);
		// }
	}

}
