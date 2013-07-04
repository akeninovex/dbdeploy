package com.dbdeploy.database;

import java.util.EnumSet;

public enum DdlObjectType {
	TABLE("TABLE", false), VIEW("VIEW", false), TRIGGER("TRIGGER", false), INDEX("INDEX", false), SEQUENCE("SEQUENCE",
			false), TYPE("TYPE", false), FUNCTION("FUNCTION", true), OTHER("OTHER", true);

	private String objectName;
	private boolean encapsulatesPlSql;

	public String getObjectName() {
		return objectName;
	}

	public boolean getEncapsulatesPlSql() {
		return encapsulatesPlSql;
	}

	private DdlObjectType(String objectName, boolean encapsulatesPlSql) {
		this.objectName = objectName;
		this.encapsulatesPlSql = encapsulatesPlSql;
	}

	public static DdlObjectType getByNameOrOTHER(String objectName) {
		for (DdlObjectType e : EnumSet.allOf(DdlObjectType.class)) {
			if (e.getObjectName().equalsIgnoreCase(objectName)) {
				return e;
			}
		}
		return OTHER;
	}

	public static boolean encapsulatesPlSql(String objectName) {
		for (DdlObjectType e : EnumSet.allOf(DdlObjectType.class)) {
			if (e.getObjectName().equalsIgnoreCase(objectName)) {
				return e.getEncapsulatesPlSql();
			}
		}
		return OTHER.getEncapsulatesPlSql();
	}
}
