package com.dbdeploy.database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrMatcher;
import org.apache.commons.lang.text.StrTokenizer;

/**
 * Adapted class that a) removes the "/" buffer executor (not needed when commands are executed via JDBC) and b) ignores
 * delimiters that occur within pl/sql blocks (i.e. within a BEGIN...END block) or elsewhere that do not constitute
 * termination of a statement. The aim is thus to provide a deploy tool that can execute scripts that are written
 * initially for use with SQL*Plus or SQL Developer.
 * 
 * @author akenworthy
 */
public class QueryStatementSplitterOracle extends QueryStatementSplitter {

	private static final String DASHES = "--";
	private static final String REM = "REM";
	private final static String BLOCK_BEGIN = "BEGIN";
	private final static String CREATE_BEGIN = "CREATE";
	private final static String DEFINITION_BEGIN = "AS";
	private final static String BLOCK_END = "END";
	private final static String BUFFER_EXEC = "/";
	private String delimiter = super.getDelimiter();
	private final static String DELIMITER_PLACEHOLDER = "_DELIMITER_PH_";
	private final static String CMD_BREAK_PLACEHOLDER = "_CMD_BREAK_PH_";
	private final static String BLOCK_END_WITH_DELIMITER_PH = BLOCK_END + DELIMITER_PLACEHOLDER;

	public List<String> split(String input) {
		System.out.println("Splitter is utilising subclass: " + this.getClass().getSimpleName());

		input = removeComments(input);

		String[] ss = getCleanedTokenArray(input);
		StringBuilder sb = getStatements(ss);

		List<String> ll = Arrays.asList(sb.toString().split(CMD_BREAK_PLACEHOLDER));
		List<String> statements = new ArrayList<String>();

		for (String l : ll) {
			String st = l.replaceAll(DELIMITER_PLACEHOLDER, delimiter).trim();
			if (StringUtils.isNotBlank(st)) {
				st = st.substring(0, st.lastIndexOf(delimiter));
				statements.add(st);
				System.out.println("Statement: " + st);
			}
		}
		return statements;
	}

	private StringBuilder getStatements(String[] ss) {
		int openBlocks = 0;
		int openCreates = 0;
		boolean definitionStarted = false;
		boolean isSimpleSqlBlock = false;
		StringBuilder sb = new StringBuilder();
		/*
		 * define statements
		 */
		for (int i = 0; i < ss.length; i++) {
			String s = ss[i];

			if (CREATE_BEGIN.equalsIgnoreCase(s)) {
				openCreates++;
			} else if (BLOCK_BEGIN.equalsIgnoreCase(s)) {
				openBlocks++;
			} else if (DEFINITION_BEGIN.equalsIgnoreCase(s)) {
				definitionStarted = true;
			} else {
				if (openCreates > 0 && !isSimpleSqlBlock && !definitionStarted) {
					if (!DdlObjectType.encapsulatesPlSql(s)) {
						/*
						 * the DDL will be treated as "simple" if only SQL, without any semicolons, follows the CREATE
						 * definition N.B. this is done by considering the object type between the CREATE statement and
						 * the first AS statement that initiates the code block
						 */
						isSimpleSqlBlock = true;
					}
				}
				/*
				 * consider more specific case first so fall-through works logically
				 */
				if (BLOCK_END_WITH_DELIMITER_PH.equalsIgnoreCase(s)) {
					if (openBlocks > 0) {
						openBlocks--;
					}
					if (openBlocks == 0) {
						/*
						 * if we have reached the final "END;" token, then the statement should be complete...
						 */
						ss[i] = s + DELIMITER_PLACEHOLDER + CMD_BREAK_PLACEHOLDER;
						isSimpleSqlBlock = false;
						definitionStarted = false;
					}
				} else if (s.toUpperCase().endsWith(DELIMITER_PLACEHOLDER)) {
					if (openBlocks == 0 && (openCreates == 0 || isSimpleSqlBlock)) {
						/*
						 * ...otherwise, if we have a "stand-alone" statement then it is terminated by the first ";"
						 * encountered
						 */
						ss[i] = s + CMD_BREAK_PLACEHOLDER;
						isSimpleSqlBlock = false;
						definitionStarted = false;
						if (openCreates > 0) {
							openCreates--;
						}
					}
				}
			}
			sb.append(StringUtils.isNotBlank(ss[i]) ? ss[i] + " " : "");
		}
		return sb;
	}

	private String[] getCleanedTokenArray(String input) {
		input = input.replaceAll(delimiter, " " + DELIMITER_PLACEHOLDER + " ");
		input = input.replaceAll(BUFFER_EXEC, "");
		/*
		 * split on (and thereby remove all) whitespace
		 */
		String[] ss = input.split("\\s+");
		/*
		 * join END with adjacent delimiter so they can be treated as single token
		 */
		joinTokens(ss);
		/*
		 * remove empty lines so that concatenation can work
		 */
		ss = removeBlanks(ss);
		return ss;
	}

	private void joinTokens(String[] ss) {
		for (int i = 0; i < ss.length; i++) {
			if (i > 0 && DELIMITER_PLACEHOLDER.equalsIgnoreCase(ss[i])) {
				ss[i - 1] = ss[i - 1] + DELIMITER_PLACEHOLDER;
				ss[i] = "";
			}
		}
	}

	/**
	 * Ignores commented lines (i.e. lines beginning with "REM" or "--")
	 */
	private String removeComments(String input) {
		StringBuilder sb = new StringBuilder();
		StrTokenizer lineTokenizer = new StrTokenizer(input);
		lineTokenizer.setDelimiterMatcher(StrMatcher.charSetMatcher("\r\n"));

		for (String line : lineTokenizer.getTokenArray()) {
			String strippedLine = StringUtils.stripEnd(line, null);
			if (!strippedLine.trim().startsWith(DASHES) && !strippedLine.trim().startsWith(REM)) {
				sb.append(strippedLine + " ");
			}
		}
		return sb.toString();
	}

	/**
	 * Removes empty elements from string array
	 */
	private String[] removeBlanks(String[] ss) {
		ArrayList<String> list = new ArrayList<String>();
		for (String s : ss) {
			if (StringUtils.isNotBlank(s)) {
				list.add(s);
			}
		}
		return list.toArray(new String[list.size()]);
	}
}
