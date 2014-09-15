package graphene.enron.web.rest;

import graphene.dao.UnifiedCommunicationEventDAO;
import graphene.enron.model.sql.enron.EnronTransactionPair100;
import graphene.model.idhelper.PropertyHelper;
import graphene.model.idl.G_Link;
import graphene.model.idl.G_Property;
import graphene.model.idl.G_PropertyTag;
import graphene.model.idl.G_PropertyType;
import graphene.model.idl.G_RelationshipType;
import graphene.model.idl.G_SearchTuple;
import graphene.model.idl.G_SearchType;
import graphene.model.query.DirectedEventQuery;
import graphene.model.query.SearchTypeHelper;
import graphene.rest.ws.EventSearchRS;
import graphene.util.FastNumberUtils;
import graphene.util.stats.TimeReporter;

import java.util.ArrayList;
import java.util.List;

import org.apache.tapestry5.ioc.annotations.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventSearchRSImpl implements EventSearchRS {

	static Logger logger = LoggerFactory.getLogger(EventSearchRSImpl.class);

	@Inject
	private UnifiedCommunicationEventDAO<EnronTransactionPair100, DirectedEventQuery> dao;

	private List<G_Link> search(DirectedEventQuery q) {
		List<G_Link> retval = new ArrayList<G_Link>();
		try {
			List<EnronTransactionPair100> list = dao.findByQuery(q);
			for (EnronTransactionPair100 k : list) {
				retval.add(convertToLink(k));
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return retval;
	}

	private G_Link convertToLink(EnronTransactionPair100 k) {
		G_Link l = new G_Link();
		List<G_Property> properties = new ArrayList<G_Property>();

		// TODO: Add this column
		// l.setSource(k.getSource());
		// the id of the target
		l.setTarget(k.getReceiverId().toString());
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
				.getTrnValueStr(), G_PropertyType.STRING));

		// TODO: Bake in the geo coords just like any other property.
		// try {
		// G_GeoData geo = new G_GeoData();
		// geo.setLat(Double.valueOf(k.getaLat()));
		// geo.setLon(Double.valueOf(k.getaLon()));
		// properties.add(new PropertyHelper("aLocation", "Sender Location",
		// geo,
		// G_PropertyType.GEO));
		// } catch (Exception e) {
		// // probably null or invalid lats/lons
		// }

		l.getTags().add(G_RelationshipType.IN_EVENT);
		l.setProperties(properties);
		l.setDirected(true);
		return l;
	}

	@Override
	public List<G_Link> getEvents(String identifiers, int offset, int limit,
			String minSecs, String maxSecs, String comments,
			boolean intersectionOnly) {
		TimeReporter t = new TimeReporter("Getting events", logger);

		if (offset < 0) {
			offset = 0;
		}
		DirectedEventQuery q = new DirectedEventQuery();
		q.setFirstResult(offset);
		q.setMaxResult(limit);
		q.setMinSecs(FastNumberUtils.parseLongWithCheck(minSecs, 0));
		q.setMaxSecs(FastNumberUtils.parseLongWithCheck(maxSecs, 0));
		q.setIntersectionOnly(intersectionOnly);

		List<G_SearchTuple<String>> tupleList = SearchTypeHelper
				.processSearchList(identifiers, G_SearchType.COMPARE_CONTAINS);

		/*
		 * Note that we are purposefully using the same list for either side of
		 * the event. The intersectionOnly flag (default false) completes the
		 * nature of the request, which is to find all events with the
		 * identifiers on either side of the event.
		 */
		q.setSources(tupleList);
		q.setDestinations(tupleList);

		q.setPayloadKeywords(SearchTypeHelper.processSearchList(comments,
				G_SearchType.COMPARE_CONTAINS));

		t.logElapsed();
		return search(q);

	}

	@Override
	public List<G_Link> getEvents(String from, String to, int offset,
			int limit, String minSecs, String maxSecs, String comments,
			boolean intersectionOnly) {
		TimeReporter t = new TimeReporter("Getting events", logger);

		if (offset < 0) {
			offset = 0;
		}
		DirectedEventQuery q = new DirectedEventQuery();
		q.setFirstResult(offset);
		q.setMaxResult(limit);
		q.setMinSecs(FastNumberUtils.parseLongWithCheck(minSecs, 0));
		q.setMaxSecs(FastNumberUtils.parseLongWithCheck(maxSecs, 0));
		/*
		 * Note that the intersectionOnly flag (default true) completes the
		 * nature of this request, which is to only show events between specific
		 * identifiers on particular sides.
		 */
		q.setIntersectionOnly(intersectionOnly);

		q.setSources(SearchTypeHelper.processSearchList(from,
				G_SearchType.COMPARE_CONTAINS));
		q.setDestinations(SearchTypeHelper.processSearchList(to,
				G_SearchType.COMPARE_CONTAINS));
		q.setPayloadKeywords(SearchTypeHelper.processSearchList(comments,
				G_SearchType.COMPARE_CONTAINS));

		List<G_Link> s = search(q);
		t.logAsCompleted();
		return s;
	}

}
