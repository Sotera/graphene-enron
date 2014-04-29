package graphene.enron.model.sql.enron;

import static com.mysema.query.types.PathMetadataFactory.*;

import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;

import com.mysema.query.sql.ColumnMetadata;




/**
 * QEnronTransactionPair100 is a Querydsl query type for EnronTransactionPair100
 */
@Generated("com.mysema.query.sql.codegen.MetaDataSerializer")
public class QEnronTransactionPair100 extends com.mysema.query.sql.RelationalPathBase<EnronTransactionPair100> {

    private static final long serialVersionUID = -1135159024;

    public static final QEnronTransactionPair100 enronTransactionPair100 = new QEnronTransactionPair100("ENRON_TRANSACTION_PAIR_1_00");

    public final NumberPath<Long> idSearchToken = createNumber("idSearchToken", Long.class);

    public final NumberPath<Integer> pairId = createNumber("pairId", Integer.class);

    public final NumberPath<Long> receiverId = createNumber("receiverId", Long.class);

    public final StringPath receiverValueStr = createString("receiverValueStr");

    public final NumberPath<Long> senderId = createNumber("senderId", Long.class);

    public final StringPath senderValueStr = createString("senderValueStr");

    public final DateTimePath<java.sql.Timestamp> trnDt = createDateTime("trnDt", java.sql.Timestamp.class);

    public final StringPath trnType = createString("trnType");

    public final NumberPath<Double> trnValueNbr = createNumber("trnValueNbr", Double.class);

    public final StringPath trnValueNbrUnit = createString("trnValueNbrUnit");

    public final StringPath trnValueStr = createString("trnValueStr");

    public final com.mysema.query.sql.PrimaryKey<EnronTransactionPair100> sysPk10092 = createPrimaryKey(pairId);

    public QEnronTransactionPair100(String variable) {
        super(EnronTransactionPair100.class, forVariable(variable), "PUBLIC", "ENRON_TRANSACTION_PAIR_1_00");
        addMetadata();
    }

    public QEnronTransactionPair100(String variable, String schema, String table) {
        super(EnronTransactionPair100.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QEnronTransactionPair100(Path<? extends EnronTransactionPair100> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "ENRON_TRANSACTION_PAIR_1_00");
        addMetadata();
    }

    public QEnronTransactionPair100(PathMetadata<?> metadata) {
        super(EnronTransactionPair100.class, metadata, "PUBLIC", "ENRON_TRANSACTION_PAIR_1_00");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(idSearchToken, ColumnMetadata.named("ID_SEARCH_TOKEN").ofType(2).withSize(15).notNull());
        addMetadata(pairId, ColumnMetadata.named("PAIR_ID").ofType(4).withSize(32).notNull());
        addMetadata(receiverId, ColumnMetadata.named("RECEIVER_ID").ofType(2).withSize(15).notNull());
        addMetadata(receiverValueStr, ColumnMetadata.named("RECEIVER_VALUE_STR").ofType(12).withSize(200));
        addMetadata(senderId, ColumnMetadata.named("SENDER_ID").ofType(2).withSize(15).notNull());
        addMetadata(senderValueStr, ColumnMetadata.named("SENDER_VALUE_STR").ofType(12).withSize(200));
        addMetadata(trnDt, ColumnMetadata.named("TRN_DT").ofType(93).withSize(26).notNull());
        addMetadata(trnType, ColumnMetadata.named("TRN_TYPE").ofType(12).withSize(200));
        addMetadata(trnValueNbr, ColumnMetadata.named("TRN_VALUE_NBR").ofType(8).withSize(64));
        addMetadata(trnValueNbrUnit, ColumnMetadata.named("TRN_VALUE_NBR_UNIT").ofType(12).withSize(200));
        addMetadata(trnValueStr, ColumnMetadata.named("TRN_VALUE_STR").ofType(12).withSize(9000));
    }

}

