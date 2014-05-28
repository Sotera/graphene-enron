package graphene.enron.web.services;

import graphene.enron.model.graphserver.InteractionFinderEnronImpl;
import graphene.enron.model.sql.enron.EnronEntityref100;
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

public class UnrolledGraphServiceTest extends ServiceTest {
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
//
//	@Test(dataProvider = "AllIds")
//	public void testPGU(String id, String degree) throws Exception {
//		System.out.println("Starting property graph test 1");
//		V_GraphQuery q = new V_GraphQuery();
//		q.setType("customer");
//		q.setMaxNodes(300);
//		q.setMaxEdgesPerNode(50);
//		q.setMaxHops(FastNumberUtils.parseIntWithCheck(degree));
//		q.addSearchIds(new String[] { id });
//		System.out.println(q);
//		try {
//			V_GenericGraph g = pgbu.makeGraphResponse(q);
//			printGraph(g);
//		} catch (Exception e) {
//			e.printStackTrace();
//			throw new Exception(e);
//		}
//
//	}

	@Test
	public void testAllTheThings() throws Exception {
		System.out.println("Starting to test all the things.");
		for (EnronEntityref100 en : this.dao.getAll(0, 10000000)) {
			V_GraphQuery q = new V_GraphQuery();
			q.setType("customer");
			q.setMaxNodes(300);
			q.setMaxEdgesPerNode(50);
			q.setMaxHops(3);
			q.addSearchIds(new String[] { en.getCustomernumber() });
			System.out.println(q);
			try {
				V_GenericGraph g = pgbu.makeGraphResponse(q);
				printGraph(g);
			} catch (Exception e) {
				e.printStackTrace();
				throw new Exception(e);
			}
		}
	}
}
