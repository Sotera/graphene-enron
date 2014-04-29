package graphene.enron.model.sql.enron;

import java.io.Serializable;
import javax.annotation.Generated;

/**
 * EnronTransactionPair100 is a Querydsl bean type
 */
@Generated("com.mysema.query.codegen.BeanSerializer")
public class EnronTransactionPair100 implements Serializable {

    private Long idSearchToken;

    private Integer pairId;

    private Long receiverId;

    private String receiverValueStr;

    private Long senderId;

    private String senderValueStr;

    private java.sql.Timestamp trnDt;

    private String trnType;

    private Double trnValueNbr;

    private String trnValueNbrUnit;

    private String trnValueStr;

    public Long getIdSearchToken() {
        return idSearchToken;
    }

    public void setIdSearchToken(Long idSearchToken) {
        this.idSearchToken = idSearchToken;
    }

    public Integer getPairId() {
        return pairId;
    }

    public void setPairId(Integer pairId) {
        this.pairId = pairId;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public String getReceiverValueStr() {
        return receiverValueStr;
    }

    public void setReceiverValueStr(String receiverValueStr) {
        this.receiverValueStr = receiverValueStr;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getSenderValueStr() {
        return senderValueStr;
    }

    public void setSenderValueStr(String senderValueStr) {
        this.senderValueStr = senderValueStr;
    }

    public java.sql.Timestamp getTrnDt() {
        return trnDt;
    }

    public void setTrnDt(java.sql.Timestamp trnDt) {
        this.trnDt = trnDt;
    }

    public String getTrnType() {
        return trnType;
    }

    public void setTrnType(String trnType) {
        this.trnType = trnType;
    }

    public Double getTrnValueNbr() {
        return trnValueNbr;
    }

    public void setTrnValueNbr(Double trnValueNbr) {
        this.trnValueNbr = trnValueNbr;
    }

    public String getTrnValueNbrUnit() {
        return trnValueNbrUnit;
    }

    public void setTrnValueNbrUnit(String trnValueNbrUnit) {
        this.trnValueNbrUnit = trnValueNbrUnit;
    }

    public String getTrnValueStr() {
        return trnValueStr;
    }

    public void setTrnValueStr(String trnValueStr) {
        this.trnValueStr = trnValueStr;
    }

    public String toString() {
         return "idSearchToken = " + idSearchToken + ", pairId = " + pairId + ", receiverId = " + receiverId + ", receiverValueStr = " + receiverValueStr + ", senderId = " + senderId + ", senderValueStr = " + senderValueStr + ", trnDt = " + trnDt + ", trnType = " + trnType + ", trnValueNbr = " + trnValueNbr + ", trnValueNbrUnit = " + trnValueNbrUnit + ", trnValueStr = " + trnValueStr;
    }

}

