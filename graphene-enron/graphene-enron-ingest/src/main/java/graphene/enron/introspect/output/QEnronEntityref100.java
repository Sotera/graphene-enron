package graphene.enron.introspect.output;

import static com.mysema.query.types.PathMetadataFactory.*;

import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;

import com.mysema.query.sql.ColumnMetadata;




/**
 * QEnronEntityref100 is a Querydsl query type for EnronEntityref100
 */
@Generated("com.mysema.query.sql.codegen.MetaDataSerializer")
public class QEnronEntityref100 extends com.mysema.query.sql.RelationalPathBase<EnronEntityref100> {

    private static final long serialVersionUID = 81003799;

    public static final QEnronEntityref100 enronEntityref100 = new QEnronEntityref100("ENRON_ENTITYREF_1_00");

    public final StringPath accountnumber = createString("accountnumber");

    public final StringPath accounttype = createString("accounttype");

    public final StringPath customernumber = createString("customernumber");

    public final StringPath customertype = createString("customertype");

    public final DateTimePath<java.sql.Timestamp> dateend = createDateTime("dateend", java.sql.Timestamp.class);

    public final DateTimePath<java.sql.Timestamp> datestart = createDateTime("datestart", java.sql.Timestamp.class);

    public final NumberPath<Integer> entityrefId = createNumber("entityrefId", Integer.class);

    public final StringPath identifier = createString("identifier");

    public final StringPath identifiercolumnsource = createString("identifiercolumnsource");

    public final StringPath identifiertablesource = createString("identifiertablesource");

    public final NumberPath<Integer> idtypeId = createNumber("idtypeId", Integer.class);

    public final com.mysema.query.sql.PrimaryKey<EnronEntityref100> sysPk10104 = createPrimaryKey(entityrefId);

    public QEnronEntityref100(String variable) {
        super(EnronEntityref100.class, forVariable(variable), "PUBLIC", "ENRON_ENTITYREF_1_00");
        addMetadata();
    }

    public QEnronEntityref100(String variable, String schema, String table) {
        super(EnronEntityref100.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QEnronEntityref100(Path<? extends EnronEntityref100> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "ENRON_ENTITYREF_1_00");
        addMetadata();
    }

    public QEnronEntityref100(PathMetadata<?> metadata) {
        super(EnronEntityref100.class, metadata, "PUBLIC", "ENRON_ENTITYREF_1_00");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(accountnumber, ColumnMetadata.named("ACCOUNTNUMBER").ofType(12).withSize(200));
        addMetadata(accounttype, ColumnMetadata.named("ACCOUNTTYPE").ofType(12).withSize(10));
        addMetadata(customernumber, ColumnMetadata.named("CUSTOMERNUMBER").ofType(12).withSize(30));
        addMetadata(customertype, ColumnMetadata.named("CUSTOMERTYPE").ofType(12).withSize(20));
        addMetadata(dateend, ColumnMetadata.named("DATEEND").ofType(93).withSize(26));
        addMetadata(datestart, ColumnMetadata.named("DATESTART").ofType(93).withSize(26));
        addMetadata(entityrefId, ColumnMetadata.named("ENTITYREF_ID").ofType(4).withSize(32).notNull());
        addMetadata(identifier, ColumnMetadata.named("IDENTIFIER").ofType(12).withSize(800).notNull());
        addMetadata(identifiercolumnsource, ColumnMetadata.named("IDENTIFIERCOLUMNSOURCE").ofType(12).withSize(100).notNull());
        addMetadata(identifiertablesource, ColumnMetadata.named("IDENTIFIERTABLESOURCE").ofType(12).withSize(100).notNull());
        addMetadata(idtypeId, ColumnMetadata.named("IDTYPE_ID").ofType(4).withSize(32));
    }

}

