package graphene.enron.ingest;

import graphene.util.FastNumberUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Email {

	String senderName;
	String senderAddress;
	String receiverName;
	String receiverAddress;
	Date dt;
	//FIXME: This is not the length, it's the payload id (the whole email, stored in another table)
	int length;
	static SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy");
	
	boolean parseFromLine(String line)
	{
		int p;
		String[]cols;
		
		cols = line.split("\t");
		
		for (int i = 0; i < cols.length; ++i)
			cols[i]=cols[i].trim();
		
		senderAddress = cols[0];
		receiverAddress = cols[1];
		
		if (senderAddress.length() == 0) {
			System.out.println("No sender in " + line);
			return false;
		}
		
		if (receiverAddress.length() == 0) {
			System.out.println("No receiver in " + line);
			return false;
		}
		
		length = FastNumberUtils.parseIntWithCheck(cols[3]);
		
		try {
			dt= df.parse(cols[2]);
		} catch (ParseException e) {
			System.out.println("Could not parse date " + cols[2]);
			return false;
		}

		return true;

	}
	public Date getDt() {
		return dt;
	}
	public void setDt(Date dt) {
		this.dt = dt;
	}
	public String getSenderName() {
		return senderName;
	}
	public void setSenderName(String senderName) {
		this.senderName = senderName;
	}
	public String getSenderAddress() {
		return senderAddress;
	}
	public void setSenderAddress(String senderAddress) {
		this.senderAddress = senderAddress;
	}
	public String getReceiverName() {
		return receiverName;
	}
	public void setReceiverName(String receiverName) {
		this.receiverName = receiverName;
	}
	public String getReceiverAddress() {
		return receiverAddress;
	}
	public void setReceiverAddress(String receiverAddress) {
		this.receiverAddress = receiverAddress;
	}
	public int getLength() {
		return length;
	}
	public void setLength(int length) {
		this.length = length;
	}
}
