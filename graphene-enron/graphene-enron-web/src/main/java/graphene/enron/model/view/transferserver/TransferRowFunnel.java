package graphene.enron.model.view.transferserver;

import graphene.enron.model.sql.enron.EnronTransactionPair100;
import graphene.model.view.events.DirectedEventRow;

import org.joda.time.DateTime;

public class TransferRowFunnel {

	public DirectedEventRow from(EnronTransactionPair100 e) {
		// TODO Auto-generated method stub
		DirectedEventRow tr = new DirectedEventRow();
		DateTime dt = new DateTime(e.getTrnDt());

		tr.setDate(e.getTrnDt());
		tr.setDay_one_based(dt.getDayOfMonth());

		// XXX: is jodatime month zero based?
		// PWG yes - fixed below

		int month = dt.getMonthOfYear();
		if (dt instanceof org.joda.time.DateTime) {
			--month;
		}
		tr.setMonth_zero_based(month);
		tr.setYear(dt.getYear());

		tr.setDateMilliSeconds(dt.getMillis()); // needed for plotting
		tr.setSenderId(e.getSenderId().toString());
		tr.addData("senderValue", e.getSenderValueStr());
		tr.addData("receiverValue", e.getReceiverValueStr());
		tr.addData(e.getTrnValueNbrUnit(), e.getTrnValueNbr().toString());
		
		tr.setReceiverId(e.getReceiverId().toString());
		tr.setUnit(e.getTrnValueNbrUnit());
		tr.setId(e.getPairId());
		tr.setComments(e.getTrnValueStr());
		tr.setDebit(e.getTrnValueNbr().doubleValue());
		return tr;

	}
}
