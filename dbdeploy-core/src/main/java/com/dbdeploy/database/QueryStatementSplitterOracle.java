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
	private final static String GRANT_BEGIN = "GRANT";
	private final static String CASE_BEGIN = "CASE";
	private final static String DEFINITION_BEGIN = "AS";
	private final static String BLOCK_END = "END";
	private final static String BUFFER_EXEC = "/";
    private static final String LOOP_BEGIN = "LOOP";
    private String delimiter = super.getDelimiter();
	private final static String DELIMITER_PLACEHOLDER = "_DELIMITER_PH_";
	private final static String CMD_BREAK_PLACEHOLDER = "_CMD_BREAK_PH_";
	private final static String BLOCK_END_WITH_DELIMITER_PH = BLOCK_END + DELIMITER_PLACEHOLDER;

	private final static String SEP = System.getProperty("line.separator");

	public List<String> split(String input) {
		System.out.println("Splitter is utilising subclass: " + this.getClass().getSimpleName());

		/*
		 * remove all comments
		 */
		input = removeBlockComments(input);
		input = removeLineEndComments(input);
		input = removeWholeLineComments(input);

		String[] ss = getCleanedTokenArray(input);
		StringBuilder sb = getStatements(ss);

		List<String> ll = Arrays.asList(sb.toString().split(CMD_BREAK_PLACEHOLDER));
		List<String> statements = new ArrayList<String>();

		for (String l : ll) {
			String st = l.replaceAll(DELIMITER_PLACEHOLDER, delimiter).trim();
			if (StringUtils.isNotBlank(st) && st.lastIndexOf(delimiter) > -1) {
				st = st.substring(0, st.lastIndexOf(delimiter));
				if (StringUtils.isNotBlank(st)) {
					statements.add(st);
					System.out.println("Statement: " + st);
				}
			}
		}
		return statements;
	}

	private StringBuilder getStatements(String[] ss) {
		int openBlocks = 0;
		int openStatement = 0;
		boolean definitionStarted = false;
		boolean isSimpleSqlBlock = false;
		StringBuilder sb = new StringBuilder();
		/*
		 * define statements
		 */
		for (int i = 0; i < ss.length; i++) {
			String s = ss[i];

			if (CREATE_BEGIN.equalsIgnoreCase(s)) {
				openStatement++;
			} else if (GRANT_BEGIN.equalsIgnoreCase(s)) {
				openStatement++;
				isSimpleSqlBlock = true;
			} else if (SqlELementType.initiatesPlSqlBlock(s)) {
				openBlocks++;
            } else if (BLOCK_END.equalsIgnoreCase(s)) {
				openBlocks--;
			} else if (DEFINITION_BEGIN.equalsIgnoreCase(s)) {
				definitionStarted = true;
			} else {
				if (openStatement > 0 && !isSimpleSqlBlock && !definitionStarted) {
					if (!DdlObjectType.encapsulatesPlSqlBlock(s)) {
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
						if (openStatement > 0) {
							openStatement--;
						}
					}
				} else if (s.toUpperCase().endsWith(DELIMITER_PLACEHOLDER)) {
					if (openBlocks == 0 && (openStatement == 0 || isSimpleSqlBlock)) {
						/*
						 * ...otherwise, if we have a "stand-alone" statement then it is terminated by the first ";"
						 * encountered
						 */
						ss[i] = s + CMD_BREAK_PLACEHOLDER;
						isSimpleSqlBlock = false;
						definitionStarted = false;
						if (openStatement > 0) {
							openStatement--;
						}
					}
				}
			}
			sb.append(StringUtils.isNotBlank(ss[i]) ? ss[i] + " " : "");
		}
		return sb;
	}

	private String[] getCleanedTokenArray(String input) {
		/*
		 * remove buffer character, but only when it is preceded or followed by a line break or colon (this
		 * distinguishes between "/" characters used in text (i.e. dates) or as divisors)
		 */
		input = input.replaceAll(BUFFER_EXEC + SEP, "");
		input = input.replaceAll(SEP + BUFFER_EXEC, "");
		input = input.replaceAll(BUFFER_EXEC + delimiter, delimiter);
		input = input.replaceAll(delimiter + BUFFER_EXEC, delimiter);
		/*
		 * replace specified command delimiters, as these will be supplied per parsed command
		 */
		input = input.replaceAll(delimiter, " " + DELIMITER_PLACEHOLDER + " ");

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

	/**
	 * Matches everything between "slash*" and "*slash". N.B. does not consider if block comments are quoted or not
	 * (this will be treated as a comment border by oracle anyway).
	 * 
	 * @param input
	 *            text from which to remove comment elements
	 * @return treated text, with comment blocks removed
	 */
	private String removeBlockComments(String input) {
		return input.replaceAll("/\\*(?:.|[\\n\\r])*?\\*/", "");
	}

	/**
	 * Ignores comments that occur at the end of a line of code: check is made to ensure that a comment (prefixed by
	 * {@link QueryStatementSplitterOracle#DASHES}) is not part of an open quote.
	 * 
	 * @param input
	 *            text to inspect
	 * @return text with end-of-line comments removed
	 */
	private String removeLineEndComments(String input) {
		StringBuilder sb = new StringBuilder();
		StrTokenizer lineTokenizer = new StrTokenizer(input);
		lineTokenizer.setDelimiterMatcher(StrMatcher.charSetMatcher(SEP));

		for (String line : lineTokenizer.getTokenArray()) {
			String strippedLine = StringUtils.stripEnd(line, null);
			int commentStarts = strippedLine.indexOf(DASHES);
			if (commentStarts > -1) {
				String preComment = strippedLine.substring(0, commentStarts);
				int quoteCount = preComment.length() - preComment.replace("'", "").length();
				if (quoteCount % 2 == 0) {
					/*
					 * comments not part of non-closed quote
					 */
					strippedLine = preComment;
				}
			}
			sb.append(strippedLine + SEP);
		}
		return sb.toString();
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
	 * Ignores commented lines (i.e. lines <b>beginning</b> with "REM" or "--")
	 */
	private String removeWholeLineComments(String input) {
		StringBuilder sb = new StringBuilder();
		StrTokenizer lineTokenizer = new StrTokenizer(input);
		lineTokenizer.setDelimiterMatcher(StrMatcher.charSetMatcher(SEP));

		for (String line : lineTokenizer.getTokenArray()) {
			String strippedLine = StringUtils.stripEnd(line, null);
			if (!strippedLine.trim().startsWith(DASHES) && !strippedLine.trim().startsWith(REM)) {
				sb.append(strippedLine + SEP);
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
