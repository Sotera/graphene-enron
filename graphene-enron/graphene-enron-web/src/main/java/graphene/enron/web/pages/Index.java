package graphene.enron.web.pages;

import graphene.model.idl.G_VisualType;
import graphene.web.annotations.PluginPage;

import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.alerts.AlertManager;
import org.apache.tapestry5.annotations.InjectComponent;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.corelib.components.Zone;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.joda.time.DateTime;

/**
 * Start page of application graphene-enron-web.
 */
@PluginPage(visualType = { G_VisualType.DEFAULT })
public class Index {
	@Property
	@Inject
	@Symbol(SymbolConstants.TAPESTRY_VERSION)
	private String tapestryVersion;

	@InjectComponent
	private Zone zone;
	
	
	@Persist
	@Property
	private int clickCount;

	@Inject
	private AlertManager alertManager;

	public DateTime getCurrentTime() {
		return new DateTime();
	}

	void onActionFromIncrement() {
		alertManager.info("Increment clicked");

		clickCount++;
	}

	Object onActionFromIncrementAjax() {
		clickCount++;

		alertManager.info("Increment (via Ajax) clicked");

		return zone;
	}
}
