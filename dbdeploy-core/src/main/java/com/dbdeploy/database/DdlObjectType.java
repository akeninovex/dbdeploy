package com.dbdeploy.database;

import java.util.EnumSet;

/**
 * Enumeration listing used/relevant DDL elements together with a property {@link #encapsulatesPlSqlBlock}
 * indicating if the DDL statement uses PL/SQL block syntax (i.e. code blocks ending with "END;" instead of a simple
 * ";". The default element ({@link #OTHER}) assumes PL/SLQ block usage.
 *
 * @author akenworthy
 */
public enum DdlObjectType {
    TABLE("TABLE", false), VIEW("VIEW", false), INDEX("INDEX", false), SEQUENCE("SEQUENCE", false), TYPE("TYPE", false), FUNCTION(
            "FUNCTION", true), TRIGGER("TRIGGER", true), OTHER("OTHER", true);

    private String objectName;
    private boolean encapsulatesPlSqlBlock;

    public String getObjectName() {
        return objectName;
    }

    public boolean getEncapsulatesPlSqlBlock() {
        return encapsulatesPlSqlBlock;
    }

    private DdlObjectType(String objectName, boolean encapsulatesPlSql) {
        this.objectName = objectName;
        this.encapsulatesPlSqlBlock = encapsulatesPlSql;
    }

    public static DdlObjectType getByNameOrOTHER(String objectName) {
        for (DdlObjectType e : EnumSet.allOf(DdlObjectType.class)) {
            if (e.getObjectName().equalsIgnoreCase(objectName)) {
                return e;
            }
        }
        return OTHER;
    }

    public static boolean encapsulatesPlSqlBlock(String objectName) {
        for (DdlObjectType e : EnumSet.allOf(DdlObjectType.class)) {
            if (e.getObjectName().equalsIgnoreCase(objectName)) {
                return e.getEncapsulatesPlSqlBlock();
            }
        }
        return OTHER.getEncapsulatesPlSqlBlock();
    }
}
