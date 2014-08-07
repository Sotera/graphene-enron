package graphene.enron.web.pages;

import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;

import graphene.model.idl.G_SymbolConstants;
import graphene.model.idl.G_VisualType;
import graphene.web.annotations.PluginPage;

@PluginPage(visualType = G_VisualType.META, menuName = "Contact", icon = "fa fa-lg fa-fw fa-info-circle")
public class Contact {
	@Property
	@Inject
	@Symbol(G_SymbolConstants.APPLICATION_CONTACT)
	private String contact;
}
