package graphene.enron.model.sql.enron;

import static com.mysema.query.types.PathMetadataFactory.*;

import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;

import com.mysema.query.sql.ColumnMetadata;




/**
 * QEnronIdentifierType100 is a Querydsl query type for EnronIdentifierType100
 */
@Generated("com.mysema.query.sql.codegen.MetaDataSerializer")
public class QEnronIdentifierType100 extends com.mysema.query.sql.RelationalPathBase<EnronIdentifierType100> {

    private static final long serialVersionUID = 1991182263;

    public static final QEnronIdentifierType100 enronIdentifierType100 = new QEnronIdentifierType100("ENRON_IDENTIFIER_TYPE_1_00");

    public final StringPath columnsource = createString("columnsource");

    public final StringPath family = createString("family");

    public final NumberPath<Integer> idtypeId = createNumber("idtypeId", Integer.class);

    public final StringPath shortName = createString("shortName");

    public final StringPath tablesource = createString("tablesource");

    public QEnronIdentifierType100(String variable) {
        super(EnronIdentifierType100.class, forVariable(variable), "PUBLIC", "ENRON_IDENTIFIER_TYPE_1_00");
        addMetadata();
    }

    public QEnronIdentifierType100(String variable, String schema, String table) {
        super(EnronIdentifierType100.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QEnronIdentifierType100(Path<? extends EnronIdentifierType100> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "ENRON_IDENTIFIER_TYPE_1_00");
        addMetadata();
    }

    public QEnronIdentifierType100(PathMetadata<?> metadata) {
        super(EnronIdentifierType100.class, metadata, "PUBLIC", "ENRON_IDENTIFIER_TYPE_1_00");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(columnsource, ColumnMetadata.named("COLUMNSOURCE").ofType(12).withSize(120));
        addMetadata(family, ColumnMetadata.named("FAMILY").ofType(12).withSize(120));
        addMetadata(idtypeId, ColumnMetadata.named("IDTYPE_ID").ofType(4).withSize(32).notNull());
        addMetadata(shortName, ColumnMetadata.named("SHORT_NAME").ofType(12).withSize(120));
        addMetadata(tablesource, ColumnMetadata.named("TABLESOURCE").ofType(12).withSize(120));
    }

}

