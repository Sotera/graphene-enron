package graphene.enron.model.memorydb;

import graphene.enron.model.sql.enron.EnronEntityref100;
import graphene.enron.model.sql.enron.EnronIdentifierType100;
import graphene.model.idl.G_CanonicalPropertyType;
import graphene.model.memorydb.AbstractMemoryDB;
import graphene.model.memorydb.MemRow;
import graphene.model.view.entities.IdType;

/**
 * This implementation is currently only for an EntityRef type table.
 * 
 * TODO: Create an interface for this, and generalize for use with non EntityRef
 * style sources.
 * 
 * XXX: There are a lot of hacks in this code that need to be addressed,
 * especially with regards to memory consumption and separating generic business
 * logic from customer implementaions.
 * 
 * @author PWG, djue
 * 
 */
public class EnronMemoryDB extends
		AbstractMemoryDB<EnronEntityref100, EnronIdentifierType100> {

	/*
	 * 
	 */
	@Override
	public boolean callBack(EnronEntityref100 p) {
		String val;
		int id;
		String identifierString;

		// Following lines are to allow for accounts created after transition
		// date
		// where there is no corresponding pre-transition number.
		// The customer number is actually the post-transition number.

		if (p.getAccountnumber() == null || p.getAccountnumber().length() == 0)
			p.setAccountnumber(p.getCustomernumber());

		// Often the same communication id occurs with and without leading zero.
		// TODO: check that this is acceptable.
		// Perhaps we should handle it when we actually look for links instead.

		identifierString = p.getIdentifier();
		IdType currentIdType = idTypeDAO.getByType(p.getIdtypeId());
		if (currentIdType == null) {
			// disabled error logging to make people feel more at ease. :-)
			// logger.error("IdType for " + p.getIdtypeId()
			// + " is null, will not load row " + p.toString());
			Integer timesEncountered = invalidTypes.get(p.getIdtypeId());
			if (timesEncountered == null) {
				timesEncountered = 1;
			} else {
				timesEncountered += 1;
			}
			invalidTypes.put(p.getIdtypeId(), timesEncountered);
			// Go ahead and continue on, by returning true for this callback
			// instance.
			return true;
		}

		/**
		 * If it's a communication id, strip a leading zero if needed.
		 * 
		 * XXX: Move this ETL decision to the database.
		 */
		if (communicationTypes.contains(currentIdType.getType())) {
			if (identifierString.startsWith("0"))
				identifierString = identifierString.substring(1);
		}

		if (state == STATE_LOAD_STRINGS) {
			// this happens first
			if (identifierString != null) {
				identifierSet.add(identifierString);
				if (currentIdType.getType() == G_CanonicalPropertyType.NAME) {
					nameSet.add(identifierString);
				} else if (communicationTypes.contains(currentIdType.getType())) {
					communicationIdSet.add(identifierString);
				}
			}
			if ((val = p.getCustomernumber()) != null) {
				customerSet.add(val);
			}
			if ((val = p.getAccountnumber()) != null) {
				accountSet.add(val);
			}
		} else {
			// Not loading strings, loading grid. this happens second.
			MemRow row = new MemRow();
			grid[currentRow] = row;
			row.setOffset(currentRow);
			row.idType = p.getIdtypeId();

			if (identifierString != null && identifierString.length() != 0) {
				id = fixLinks(grid, identifierString, identifiers, IDENTIFIER);
				row.entries[IDENTIFIER] = id;
			}

			val = p.getCustomernumber();
			if (val != null && val.length() != 0) {
				id = fixLinks(grid, val, customers, CUSTOMER);
				row.entries[CUSTOMER] = id;
			}

			val = p.getAccountnumber();
			if (val != null && val.length() != 0) {
				id = fixLinks(grid, val, accounts, ACCOUNT);
				row.entries[ACCOUNT] = id;
			}

			currentRow++;
		}
		numProcessed++;
		return true;
	}
}