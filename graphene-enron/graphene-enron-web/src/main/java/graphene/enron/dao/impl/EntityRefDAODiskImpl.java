package graphene.enron.dao.impl;

import graphene.dao.EntityRefDAO;
import graphene.enron.model.sql.enron.EnronEntityref100;
import graphene.model.query.EntityRefQuery;
import graphene.util.CallBack;
import graphene.util.fs.DiskCache;
import graphene.util.fs.ObjectStreamIterator;
import graphene.util.stats.TimeReporter;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.ServiceId;
import org.slf4j.Logger;

@ServiceId("Disk")
public class EntityRefDAODiskImpl extends EntityRefDAOImpl implements
		EntityRefDAO<EnronEntityref100, EntityRefQuery> {
	private static final boolean DELETE_EXISTING = false;
	private static final String FILE_NAME = "T:/data/entityRefCache.data";

	private static final long MAX_RESULTS = 0;

	private static final boolean SAVE_LOCALLY = true;

	private static final boolean TRY_SERIALIZED = true;

	@Inject
	private DiskCache<EnronEntityref100> diskCache;
	@Inject
	private Logger logger;

	private long numRows = 0;

	@Override
	public long count() throws Exception {
		TimeReporter t = new TimeReporter("Counting all rows", logger);
		ObjectStreamIterator<EnronEntityref100> iter = getIterator(
				DELETE_EXISTING, TRY_SERIALIZED, SAVE_LOCALLY, 0);
		EnronEntityref100 e;
		if (numRows == 0) {
			while (iter.hasNext() && (e = iter.next()) != null) {
				numRows++;
				if (numRows % 1000 == 0) {
					t.getSpeed(numRows, "Count EnronEntityref100");
				}
			}
			iter.close();
		}
		return numRows;
	}

	public ObjectStreamIterator<EnronEntityref100> getIterator(
			boolean deleteExisting, boolean trySerialized, boolean saveLocally,
			long maxResults) {
		ObjectInputStream s = null;
		if (trySerialized) {
			s = diskCache.restoreFromDisk(FILE_NAME);
			if (s != null) {
				logger.debug("Serialized version found! Reusing existing serialized version.");
				return new ObjectStreamIterator<EnronEntityref100>(s);
			} else {
				logger.debug("Serialized version not found, will need to read from DB");
			}
		} else if (deleteExisting) {
			if (diskCache.dropExisting(FILE_NAME)) {
				logger.debug("Deleted existing file");
			} else {
				logger.warn("Could not delete existing file");
			}
		}
		// this is not in an else block because we read from the db as a
		// secondary option.
		TimeReporter tr = new TimeReporter("Loading from database", logger);

		if (saveLocally ) {

			ObjectOutputStream objStream = diskCache.getObjectStream(FILE_NAME);

			try {
				//Note that the maxValue is the highest index number.
				super.throttlingCallbackOnValues(0, maxResults, diskCache,
						null, 500000, 500000, 800000, 0,
						super.getMaxIndexValue());
			} catch (Exception e) {
				logger.error(e.getMessage());
				e.printStackTrace();
			}
			diskCache.closeStreams();
		} else {
			logger.error("Could not read from database!");
		}
		tr.logAsCompleted();
		s = diskCache.restoreFromDisk(FILE_NAME);
		return new ObjectStreamIterator<EnronEntityref100>(s);
	}

	@Override
	public boolean performCallback(long offset, long maxResults,
			CallBack<EnronEntityref100> cb, EntityRefQuery q) {
		logger.debug("Acquiring data to iterate over...");
		ObjectStreamIterator<EnronEntityref100> iter = getIterator(
				DELETE_EXISTING, TRY_SERIALIZED, SAVE_LOCALLY, maxResults);
		
		EnronEntityref100 e;
		long numProcessed = 0;
		TimeReporter t = new TimeReporter("Performing callbacks on all rows",
				logger);
		while (iter.hasNext() && (e = iter.next()) != null) {
			numProcessed++;
			cb.callBack(e);
			if (numProcessed % 1000 == 0) {
				t.getSpeed(numProcessed, "EnronEntityref100");
			}
		}
		if (numRows == 0) {
			// Update the number of rows processed so we don't have to count
			// them later. Assumes all rows.
			numRows = numProcessed;
		}
		t.logAsCompleted();
		return true;
	}

}
