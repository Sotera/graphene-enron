package graphene.enron.web.services;

import graphene.enron.model.graphserver.InteractionFinderEnronImpl;
import graphene.util.FastNumberUtils;
import mil.darpa.vande.generic.V_GenericEdge;
import mil.darpa.vande.generic.V_GenericGraph;
import mil.darpa.vande.generic.V_GenericNode;
import mil.darpa.vande.generic.V_GraphQuery;
import mil.darpa.vande.interactions.InteractionFinder;
import mil.darpa.vande.interactions.InteractionGraphBuilder;
import mil.darpa.vande.interactions.TemporalGraphQuery;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class GraphServiceTest extends ServiceTest {
	@DataProvider
	public Object[][] AllIds() {

		return new Object[][] { { "1", "3" }, { "2", "3" }, { "3", "3" },
				{ "4", "3" }, { "5", "3" }, { "6", "3" }, { "7", "3" },
				{ "8", "3" }, { "9", "3" }, { "10", "3" }, { "11", "3" },
				{ "12", "3" }, { "13", "3" }, { "14", "3" }, { "15", "3" },
				{ "16", "3" }, { "17", "3" }, { "18", "3" }, { "19", "3" },
				{ "20", "3" }, { "21", "3" }, { "22", "3" }, { "23", "3" },
				{ "24", "3" }, { "25", "3" }, { "26", "3" }, { "27", "3" },
				{ "28", "3" }, { "29", "3" }, { "30", "3" }, { "31", "3" },
				{ "32", "3" }, { "33", "3" }, { "34", "3" }, { "73", "3" } };
	}

	@BeforeTest
	public void beforeTest() throws Exception {

	}

	@Test(dataProvider = "AllIds")
	public void testPG(String id, String degree) throws Exception {
		System.out.println("Starting property graph test 1");
		V_GraphQuery q = new V_GraphQuery();
		q.setType("customer");
		q.setMaxNodes(300);
		q.setMaxEdgesPerNode(50);
		q.setMaxHops(FastNumberUtils.parseIntWithCheck(degree));
		q.addSearchIds(new String[] { id });
		System.out.println(q);
		try {
			V_GenericGraph g = pgb.makeGraphResponse(q, propertyFinder);
			printGraph(g);
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception(e);
		}

	}

	@Test(dataProvider = "AllIds")
	public void testIG(String id, String degree) throws Exception {
		System.out.println("Starting interaction test 1");
		V_GraphQuery q = new V_GraphQuery();
		q.setType("customer");
		q.setMaxNodes(300);
		q.setMaxEdgesPerNode(50);
		q.setMaxHops(FastNumberUtils.parseIntWithCheck(degree));
		q.addSearchIds(new String[] { id });
		System.out.println(q);
		igb.setOriginalQuery(q);
		try {
			V_GenericGraph g = igb.makeGraphResponse(q, interactionFinder);
			printGraph(g);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test(dataProvider = "AllIds")
	public void testIG2(String id, String degree) throws Exception {
		System.out.println("Starting temporal interaction test 1");
		TemporalGraphQuery gq = new TemporalGraphQuery();
		long startDate = FastNumberUtils.parseLongWithCheck("0", 0);
		long endDate = FastNumberUtils.parseLongWithCheck("" + Long.MAX_VALUE,
				0);
		gq.setMaxHops(FastNumberUtils.parseIntWithCheck(degree));
		gq.setMaxNodes(5);
		gq.setMaxEdgesPerNode(5);
		gq.setMinLinks(2);
		gq.setMinTransValue(0);
		gq.setByMonth(false);
		gq.setByDay(true);
		gq.setByYear(false);
		gq.setDirected(true);
		gq.setStartTime(startDate);
		gq.setEndTime(endDate);
		gq.addSearchIds(new String[] { id });

		System.out.println(gq);

		InteractionFinder finder = new InteractionFinderEnronImpl(
				this.transactionDAO);
		InteractionGraphBuilder b = new InteractionGraphBuilder();
		b.setOriginalQuery(gq);
		V_GenericGraph g = b.makeGraphResponse(gq, finder);

		logger.debug("Made graph with " + g.getNodes().size() + " Nodes and "
				+ g.getEdges().size() + " Edges");

		printGraph(g);
		// V_CSGraph m = new V_CSGraph(g, true);
		// System.out.println(g.toString());

	}

	private void printGraph(V_GenericGraph g) {
		System.out.println("=====================");
		for (V_GenericNode x : g.getNodes()) {
			System.out.println(x);
		}
		for (V_GenericEdge x : g.getEdges()) {
			System.out.println(x);
		}
		System.out.println("=====================");
	}
}
