package graphene.enron.web.pages;

import graphene.model.idl.G_SymbolConstants;
import graphene.model.idl.G_VisualType;
import graphene.web.annotations.PluginPage;
import graphene.web.pages.SimpleBasePage;

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
@PluginPage(visualType = G_VisualType.TOP, menuName = "Enron Dashboard", icon = "fa fa-lg fa-fw fa-code-home")
public class Index extends SimpleBasePage {
	@Property
	@Inject
	@Symbol(SymbolConstants.TAPESTRY_VERSION)
	private String tapestryVersion;
	@Property
	@Inject
	@Symbol(G_SymbolConstants.APPLICATION_NAME)
	private String appName;
	@Inject
	private AlertManager alertManager;

	public DateTime getCurrentTime() {
		return new DateTime();
	}

}
