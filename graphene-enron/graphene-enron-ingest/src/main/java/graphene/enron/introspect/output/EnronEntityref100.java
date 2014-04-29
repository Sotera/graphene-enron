package graphene.enron.introspect.output;

import java.io.Serializable;
import javax.annotation.Generated;

/**
 * EnronEntityref100 is a Querydsl bean type
 */
@Generated("com.mysema.query.codegen.BeanSerializer")
public class EnronEntityref100 implements Serializable {

    private String accountnumber;

    private String accounttype;

    private String customernumber;

    private String customertype;

    private java.sql.Timestamp dateend;

    private java.sql.Timestamp datestart;

    private Integer entityrefId;

    private String identifier;

    private String identifiercolumnsource;

    private String identifiertablesource;

    private Integer idtypeId;

    public String getAccountnumber() {
        return accountnumber;
    }

    public void setAccountnumber(String accountnumber) {
        this.accountnumber = accountnumber;
    }

    public String getAccounttype() {
        return accounttype;
    }

    public void setAccounttype(String accounttype) {
        this.accounttype = accounttype;
    }

    public String getCustomernumber() {
        return customernumber;
    }

    public void setCustomernumber(String customernumber) {
        this.customernumber = customernumber;
    }

    public String getCustomertype() {
        return customertype;
    }

    public void setCustomertype(String customertype) {
        this.customertype = customertype;
    }

    public java.sql.Timestamp getDateend() {
        return dateend;
    }

    public void setDateend(java.sql.Timestamp dateend) {
        this.dateend = dateend;
    }

    public java.sql.Timestamp getDatestart() {
        return datestart;
    }

    public void setDatestart(java.sql.Timestamp datestart) {
        this.datestart = datestart;
    }

    public Integer getEntityrefId() {
        return entityrefId;
    }

    public void setEntityrefId(Integer entityrefId) {
        this.entityrefId = entityrefId;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifiercolumnsource() {
        return identifiercolumnsource;
    }

    public void setIdentifiercolumnsource(String identifiercolumnsource) {
        this.identifiercolumnsource = identifiercolumnsource;
    }

    public String getIdentifiertablesource() {
        return identifiertablesource;
    }

    public void setIdentifiertablesource(String identifiertablesource) {
        this.identifiertablesource = identifiertablesource;
    }

    public Integer getIdtypeId() {
        return idtypeId;
    }

    public void setIdtypeId(Integer idtypeId) {
        this.idtypeId = idtypeId;
    }

    public String toString() {
         return "accountnumber = " + accountnumber + ", accounttype = " + accounttype + ", customernumber = " + customernumber + ", customertype = " + customertype + ", dateend = " + dateend + ", datestart = " + datestart + ", entityrefId = " + entityrefId + ", identifier = " + identifier + ", identifiercolumnsource = " + identifiercolumnsource + ", identifiertablesource = " + identifiertablesource + ", idtypeId = " + idtypeId;
    }

}

