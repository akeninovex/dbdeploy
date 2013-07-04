package com.dbdeploy.database;

public enum DelimiterType {
	/**
	 * Delimiter is interpreted whenever it appears at the end of a line
	 */
	normal {
		public boolean matches(String line, String delimiter) {
			return line.endsWith(delimiter);
		}
	},

	/**
	 * Delimiter must be on a line all to itself
	 */
	row {
		public boolean matches(String line, String delimiter) {
			return line.equals(delimiter);
		}
	},

	/**
	 * Delimiters will be determined by parsing oracle sql/plsql
	 */
	oracle_parsed {
		public boolean matches(String line, String delimiter) {
			return false;
		}
	};

	public abstract boolean matches(String line, String delimiter);
}
