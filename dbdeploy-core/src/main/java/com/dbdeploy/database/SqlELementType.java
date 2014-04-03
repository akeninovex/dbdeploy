package com.dbdeploy.database;

import java.util.EnumSet;

/**
 * Enumeration listing used/relevant SQL text elements that initiate (and thereby necessitate termination of) PlSql blocks of code,
 * indicated by property {@link #initiatesPlSqlBlock}. The default element ({@link #OTHER}) assumes no PL/SLQ block is initiated.
 *
 * @author akenworthy
 */
public enum SqlELementType {
    CASE("CASE", true), LOOP("LOOP", true), BEGIN("BEGIN", true), OTHER("OTHER", false);

    private String objectName;
    private boolean initiatesPlSqlBlock;

    public String getObjectName() {
        return objectName;
    }

    public boolean getInitiatesPlSqlBlock() {
        return initiatesPlSqlBlock;
    }

    private SqlELementType(String objectName, boolean initiatesPlSqlBlock) {
        this.objectName = objectName;
        this.initiatesPlSqlBlock = initiatesPlSqlBlock;
    }

    public static SqlELementType getByNameOrOTHER(String objectName) {
        for (SqlELementType e : EnumSet.allOf(SqlELementType.class)) {
            if (e.getObjectName().equalsIgnoreCase(objectName)) {
                return e;
            }
        }
        return OTHER;
    }

    public static boolean initiatesPlSqlBlock(String objectName) {
        for (SqlELementType e : EnumSet.allOf(SqlELementType.class)) {
            if (e.getObjectName().equalsIgnoreCase(objectName)) {
                return e.getInitiatesPlSqlBlock();
            }
        }
        return OTHER.getInitiatesPlSqlBlock();
    }
}
