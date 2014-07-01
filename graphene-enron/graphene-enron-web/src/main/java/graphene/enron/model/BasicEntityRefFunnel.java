package graphene.enron.model;

import graphene.enron.model.sql.enron.EnronEntityref100;
import graphene.model.Funnel;
import graphene.model.view.entities.BasicEntityRef;

import org.apache.commons.lang3.time.DateFormatUtils;

public class BasicEntityRefFunnel implements
		Funnel<BasicEntityRef, EnronEntityref100> {

	@Override
	public BasicEntityRef from(EnronEntityref100 f) {
		BasicEntityRef b = new BasicEntityRef();
		b.setAccountNumber(f.getAccountnumber());
		b.setAccountType(f.getAccounttype());
		b.setCustomerNumber(f.getCustomernumber());
		b.setCustomerType(f.getCustomertype());

		b.setDateStart(DateFormatUtils.ISO_DATE_FORMAT.format(f.getDatestart()));
		b.setDateEnd(DateFormatUtils.ISO_DATE_FORMAT.format(f.getDateend()));

		b.setEntityrefId(f.getEntityrefId());

		b.setIdentifier(f.getIdentifier());

		b.setIdentifierColumnSource(f.getIdentifiercolumnsource());
		b.setIdentifierTableSource(f.getIdentifiertablesource());
		b.setIdtypeId(f.getIdtypeId());

		return b;
	}
}
