package graphene.enron.model;

import graphene.enron.model.sql.enron.EnronTransactionPair100;
import graphene.model.Funnel;
import graphene.model.idl.G_CanonicalRelationshipType;
import graphene.model.idl.G_EdgeType;
import graphene.model.idl.G_Link;
import graphene.model.idl.G_LinkTag;
import graphene.model.idl.G_Property;
import graphene.model.idl.G_PropertyTag;
import graphene.model.idl.G_PropertyType;
import graphene.model.idlhelper.LinkHelper;
import graphene.model.idlhelper.PropertyHelper;

import java.util.ArrayList;
import java.util.List;

public class G_LinkFunnel implements Funnel<EnronTransactionPair100, G_Link> {

	@Override
	public EnronTransactionPair100 to(G_Link f) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public G_Link from(EnronTransactionPair100 k) {

		List<G_Property> properties = new ArrayList<G_Property>();
		// Party A
		properties.add(new PropertyHelper("aName", "Sender Name", k
				.getSenderId(), G_PropertyType.STRING, G_PropertyTag.NAME));
		properties.add(new PropertyHelper("aId", "Sender Id", k.getSenderId(),
				G_PropertyType.STRING, G_PropertyTag.ID));

		// Party B
		properties.add(new PropertyHelper("aName", "Receiver Name", k
				.getReceiverValueStr(), G_PropertyType.STRING,
				G_PropertyTag.NAME));
		properties.add(new PropertyHelper("aId", "Receiver Id", k
				.getReceiverId(), G_PropertyType.STRING, G_PropertyTag.ID));

		// Common
		properties.add(new PropertyHelper("Comments", "Comments", k
				.getTrnValueStr(), G_PropertyType.STRING, G_PropertyTag.TEXT));

		G_Link l = new LinkHelper(G_LinkTag.COMMUNICATION, k.getSenderId()
				.toString(), k.getReceiverId().toString(), properties);
		l.setDirected(true);
		return l;
	}
}
