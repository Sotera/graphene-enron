package graphene.enron.model.sql.enron;

import java.io.Serializable;
import javax.annotation.Generated;

/**
 * EnronIdentifierType100 is a Querydsl bean type
 */
@Generated("com.mysema.query.codegen.BeanSerializer")
public class EnronIdentifierType100 implements Serializable {

    private String columnsource;

    private String family;

    private Integer idtypeId;

    private String shortName;

    private String tablesource;

    public String getColumnsource() {
        return columnsource;
    }

    public void setColumnsource(String columnsource) {
        this.columnsource = columnsource;
    }

    public String getNodeType() {
        return family;
    }

    public void setFamily(String family) {
        this.family = family;
    }

    public Integer getIdtypeId() {
        return idtypeId;
    }

    public void setIdtypeId(Integer idtypeId) {
        this.idtypeId = idtypeId;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getTablesource() {
        return tablesource;
    }

    public void setTablesource(String tablesource) {
        this.tablesource = tablesource;
    }

    public String toString() {
         return "columnsource = " + columnsource + ", family = " + family + ", idtypeId = " + idtypeId + ", shortName = " + shortName + ", tablesource = " + tablesource;
    }

}

