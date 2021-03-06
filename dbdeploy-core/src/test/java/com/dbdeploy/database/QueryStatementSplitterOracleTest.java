package com.dbdeploy.database;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class QueryStatementSplitterOracleTest {
	private QueryStatementSplitter oraSplitter;

	public static final long ONE_DAY_IN_MS = 86400000L;

	public static long getTimeOfTheDayInMs() {
		return System.currentTimeMillis() % ONE_DAY_IN_MS;
	}

	@Before
	public void setUp() throws Exception {
		oraSplitter = new QueryStatementSplitterOracle();
	}

	@Test
	public void oracleNestedBlocks() throws Exception {
		List<String> result = oraSplitter
				.split(" select 0 from dual; begin \n select 1 from dual; \n begin select 2 from dual; end \n ; end;");
		assertThat(result, hasItem("select 0 from dual"));
		assertThat(result, hasItem("begin select 1 from dual; begin select 2 from dual; end; end;"));
		assertThat(result.size(), is(2));

		System.out.println(new Date(System.currentTimeMillis()));
		System.out.println(new Date(getTimeOfTheDayInMs()));

		System.out.println(new Date(System.currentTimeMillis()));
		System.out.println(new Date(getTimeOfTheDayInMs()));
	}

	@Test
	public void oracleLeaveDots() throws Exception {
		List<String> result = oraSplitter.split("create sequence x.y\n;");
		assertThat(result, hasItem("create sequence x.y"));
		assertThat(result.size(), is(1));
	}

	@Test
	public void oracleIgnoreComments() throws Exception {
		List<String> result = oraSplitter.split("-- here is my comment \n create sequence x.y\n;");
		assertThat(result, hasItem("create sequence x.y"));
		assertThat(result.size(), is(1));
	}
	
	@Test
	public void oracleIgnoreCommentsEndOfLine() throws Exception {
		List<String> result = oraSplitter.split("create sequence x.y;-- here is my comment");
		assertThat(result, hasItem("create sequence x.y"));
		assertThat(result.size(), is(1));
	}
	
	@Test
	public void oracleIgnoreCommentsEndOfLine2() throws Exception {
		List<String> result = oraSplitter.split("select 1-- here is my comment\n from dual;");
		assertThat(result, hasItem("select 1 from dual"));
		assertThat(result.size(), is(1));
	}
	
	@Test
	public void oracleIgnoreCommentsExceptQuoted() throws Exception {
		List<String> result = oraSplitter.split("select ' quoted hyphens -- ' from dual;");
		assertThat(result, hasItem("select ' quoted hyphens -- ' from dual"));
		assertThat(result.size(), is(1));
	}

	@Test
	public void oracleIgnoreCommentsBlock() throws Exception {
		List<String> result = oraSplitter.split("/* here is my comment */\n create sequence x.y\n;");
		assertThat(result, hasItem("create sequence x.y"));
		assertThat(result.size(), is(1));
	}

	@Test
	public void oracleIgnoreCommentsBlockOverLines() throws Exception {
		List<String> result = oraSplitter
				.split("/* here is my \n comment over multiple \n lines*/\n create sequence x.y\n;");
		assertThat(result, hasItem("create sequence x.y"));
		assertThat(result.size(), is(1));
	}

	@Test
	public void oracleIgnoreCommentsBlocksOverLines() throws Exception {
		List<String> result = oraSplitter.split("/* here is my \n comment over multiple \n lines*/\n create "
				+ "/* here is another \n comment over multiple \n lines right in the middle of a command*/"
				+ "sequence x.y\n;");
		assertThat(result, hasItem("create sequence x.y"));
		assertThat(result.size(), is(1));
	}

	@Test
	public void oracleAdjacentStatementsNoWhitespace() throws Exception {
		List<String> result = oraSplitter.split("select 1 from dual;select 2 from dual;");
		assertThat(result, hasItem("select 1 from dual"));
		assertThat(result, hasItem("select 2 from dual"));
		assertThat(result.size(), is(2));
	}

	@Test
	public void oracleTrigger() throws Exception {
		List<String> result = oraSplitter.split("create trigger x.y\n BEFORE INSERT ON x.y \nFOR EACH ROW "
				+ "\nBEGIN\n:new.id := x.y.z;\nEND;/");
		assertThat(result, hasItem("create trigger x.y BEFORE INSERT ON x.y FOR EACH ROW "
				+ "BEGIN :new.id := x.y.z; END;"));

		assertThat(result.size(), is(1));
	}

	@Test
	public void oracleTable() throws Exception {
		List<String> result = oraSplitter.split("CREATE table blahblah (v_long varchar2(4000));");
		assertThat(result, hasItem("CREATE table blahblah (v_long varchar2(4000))"));

		assertThat(result.size(), is(1));
	}

	@Test
	public void oracleFunction() throws Exception {
		List<String> result = oraSplitter
				.split("CREATE FUNCTION as v_long varchar2(4000); BEGIN\nselect 1 from dual;\nEND;/");
		assertThat(result, hasItem("CREATE FUNCTION as v_long varchar2(4000); BEGIN select 1 from dual; END;"));

		assertThat(result.size(), is(1));
	}

	@Test
	public void oracleFunctionNestedBlocks() throws Exception {
		List<String> result = oraSplitter
				.split("CREATE FUNCTION as v_long varchar2(4000);BEGIN select 1 from dual;BEGIN select 2 from dual;END;END;");
		assertThat(
				result,
				hasItem("CREATE FUNCTION as v_long varchar2(4000); BEGIN select 1 from dual; BEGIN select 2 from dual; END; END;"));

		assertThat(result.size(), is(1));
	}

    @Test
    public void oracleFunctionNestedLoops() throws Exception {
        List<String> result = oraSplitter
                .split("create function x.y(p in varchar2) return z as begin loop loop\n" +
                        "end loop; end loop; return x; end;");
        assertThat(
                result,
                hasItem("create function x.y(p in varchar2) return z as begin loop loop end loop; end loop; return x; end;"));

        assertThat(result.size(), is(1));
    }

	@Test
	public void oracleFunctionWithTypes() throws Exception {
		List<String> result = oraSplitter
				.split("create or replace type x.y as object (col varchar2(200));"
						+ "create or replace type x as table of y;/"
						+ "create or replace function x(a in varchar2, b in varchar2) return y as l_a := varchar2(10);l_b := varchar2(10);"
						+ "begin select 1 from dual; select 2 from dual;end;/");
		assertThat(
				result,
				hasItem("create or replace function x(a in varchar2, b in varchar2) return y as l_a := varchar2(10); l_b := varchar2(10); "
						+ "begin select 1 from dual; select 2 from dual; end;"));
		assertThat(result, hasItem("create or replace type x as table of y"));
		assertThat(result, hasItem("create or replace type x.y as object (col varchar2(200))"));

		assertThat(result.size(), is(3));
	}

	@Test
	public void oracleFunctionWithTypesAndGrants() throws Exception {
		List<String> result = oraSplitter
				.split("create or replace type x.y as object (col varchar2(200));"
						+ "create or replace type x as table of y;/"
						+ "create or replace function x(a in varchar2, b in varchar2) return y as l_a := varchar2(10);l_b := varchar2(10);"
						+ "begin select 1 from dual; select 2 from dual;end;/grant all on x to y;grant all on y to x;");
		assertThat(
				result,
				hasItem("create or replace function x(a in varchar2, b in varchar2) return y as l_a := varchar2(10); l_b := varchar2(10); "
						+ "begin select 1 from dual; select 2 from dual; end;"));
		assertThat(result, hasItem("create or replace type x as table of y"));
		assertThat(result, hasItem("create or replace type x.y as object (col varchar2(200))"));
		assertThat(result, hasItem("grant all on x to y"));
		assertThat(result, hasItem("grant all on y to x"));

		assertThat(result.size(), is(5));
	}

	@Test
	public void oracleFunctionComplexWithTypes() throws Exception {
		List<String> result = oraSplitter
				.split("create or replace type y.x as object (col varchar2(200));"
						+ "create or replace type x as table of y;/"
						+ "create or replace function x(p_user in varchar2, p_table in varchar2) return y as "
						+ "l_data length_metric_table := length_metric_table();type col_tbl is table of all_tab_columns.column_name%type; l_col_names col_tbl;"
						+ "begin select 1 from dual; select 2 from dual;end;/");
		assertThat(result, hasItem("create or replace type x as table of y"));
		assertThat(result, hasItem("create or replace type y.x as object (col varchar2(200))"));
		assertThat(
				result,
				hasItem("create or replace function x(p_user in varchar2, p_table in varchar2) return y as "
						+ "l_data length_metric_table := length_metric_table(); type col_tbl is table of all_tab_columns.column_name%type; "
						+ "l_col_names col_tbl; begin select 1 from dual; select 2 from dual; end;"));

		assertThat(result.size(), is(3));
	}

	@Test
	public void oracleIndexes() throws Exception {
		List<String> result = oraSplitter
				.split("CREATE table blahblah (v_long varchar2(4000));\nALTER TABLE x.y ADD CONSTRAINT z PRIMARY KEY\n(\nid\n)USING INDEX;"
						+ "CREATE UNIQUE INDEX x.y on x.y(c);");

		assertThat(result, hasItem("CREATE table blahblah (v_long varchar2(4000))"));
		assertThat(result, hasItem("ALTER TABLE x.y ADD CONSTRAINT z PRIMARY KEY ( id )USING INDEX"));
		assertThat(result, hasItem("CREATE UNIQUE INDEX x.y on x.y(c)"));

		assertThat(result.size(), is(3));
	}

	@Test
	public void oracleIndexesReverse() throws Exception {
		List<String> result = oraSplitter
				.split("CREATE table blahblah (v_long varchar2(4000));CREATE UNIQUE INDEX x.y on x.y(z);\n"
						+ "ALTER TABLE x.y ADD CONSTRAINT z PRIMARY KEY\n(\nid\n)USING INDEX;");

		assertThat(result, hasItem("CREATE table blahblah (v_long varchar2(4000))"));
		assertThat(result, hasItem("ALTER TABLE x.y ADD CONSTRAINT z PRIMARY KEY ( id )USING INDEX"));
		assertThat(result, hasItem("CREATE UNIQUE INDEX x.y on x.y(z)"));

		assertThat(result.size(), is(3));
	}

	@Test
	public void oracleView() throws Exception {
		List<String> result = oraSplitter
				.split("CREATE view blahblah1 as select 1 from dual;CREATE view blahblah2 as select 2 from dual;");

		assertThat(result, hasItem("CREATE view blahblah1 as select 1 from dual"));
		assertThat(result, hasItem("CREATE view blahblah2 as select 2 from dual"));

		assertThat(result.size(), is(2));
	}

	@Test
	public void oracleViewPlus() throws Exception {
		List<String> result = oraSplitter
				.split("CREATE view blahblah1 as select 1 from dual;CREATE FUNCTION as v_long varchar2(4000);BEGIN select 1 from dual;BEGIN select 2 from dual;END;END;");

		assertThat(result, hasItem("CREATE view blahblah1 as select 1 from dual"));
		assertThat(
				result,
				hasItem("CREATE FUNCTION as v_long varchar2(4000); BEGIN select 1 from dual; BEGIN select 2 from dual; END; END;"));

		assertThat(result.size(), is(2));
	}

	@Test
	public void oracleViewPlusReverse() throws Exception {
		List<String> result = oraSplitter
				.split("CREATE FUNCTION as v_long varchar2(4000);BEGIN select 1 from dual;BEGIN select 2 from dual;END;END;CREATE view blahblah1 as select 1 from dual;");

		assertThat(result, hasItem("CREATE view blahblah1 as select 1 from dual"));
		assertThat(
				result,
				hasItem("CREATE FUNCTION as v_long varchar2(4000); BEGIN select 1 from dual; BEGIN select 2 from dual; END; END;"));

		assertThat(result.size(), is(2));
	}

	@Test
	public void oracleMaterializedView() throws Exception {
		List<String> result = oraSplitter
				.split("CREATE materialized view blahblah1 refresh complete as select 1 from dual;");

		assertThat(result, hasItem("CREATE materialized view blahblah1 refresh complete as select 1 from dual"));

		assertThat(result.size(), is(1));
	}

	@Test
	public void oracleMaterializedViewsPlus() throws Exception {
		List<String> result = oraSplitter
				.split("CREATE materialized view blahblah1 refresh complete as select 1 from dual;CREATE materialized view blahblah2 refresh complete as select 2 from dual;CREATE FUNCTION as v_long varchar2(4000);BEGIN select 1 from dual;BEGIN select 2 from dual;END;END;");

		assertThat(result, hasItem("CREATE materialized view blahblah1 refresh complete as select 1 from dual"));
		assertThat(result, hasItem("CREATE materialized view blahblah2 refresh complete as select 2 from dual"));
		assertThat(
				result,
				hasItem("CREATE FUNCTION as v_long varchar2(4000); BEGIN select 1 from dual; BEGIN select 2 from dual; END; END;"));

		assertThat(result.size(), is(3));
	}

	@Test
	public void oracleMaterializedViewsPlusReverse() throws Exception {
		List<String> result = oraSplitter
				.split("CREATE FUNCTION as v_long varchar2(4000);BEGIN select 1 from dual;BEGIN select 2 from dual;END;END;CREATE materialized view blahblah1 refresh complete as select 1 from dual;CREATE materialized view blahblah2 refresh complete as select 2 from dual;");

		assertThat(result, hasItem("CREATE materialized view blahblah1 refresh complete as select 1 from dual"));
		assertThat(result, hasItem("CREATE materialized view blahblah2 refresh complete as select 2 from dual"));
		assertThat(
				result,
				hasItem("CREATE FUNCTION as v_long varchar2(4000); BEGIN select 1 from dual; BEGIN select 2 from dual; END; END;"));

		assertThat(result.size(), is(3));
	}

	@Test
	public void oracleMaterializedViews() throws Exception {
		List<String> result = oraSplitter
				.split("CREATE materialized view blahblah1 refresh complete as select 1 from dual;CREATE materialized view blahblah2 refresh complete as select 2 from dual;");

		assertThat(result, hasItem("CREATE materialized view blahblah1 refresh complete as select 1 from dual"));
		assertThat(result, hasItem("CREATE materialized view blahblah2 refresh complete as select 2 from dual"));

		assertThat(result.size(), is(2));
	}

	@Test
	public void oracleTableAs() throws Exception {
		List<String> result = oraSplitter
				.split("create table x as select 1 from dual;create view blahblah1 as select 1 from dual;");

		assertThat(result, hasItem("create view blahblah1 as select 1 from dual"));
		assertThat(result, hasItem("create table x as select 1 from dual"));

		assertThat(result.size(), is(2));
	}

	@Test
	public void oracleTableAsWithCTE() throws Exception {
		List<String> result = oraSplitter
				.split("create table x as with basedata as (select 1 from dual) select * from basedata;create view blahblah1 as select 1 from dual;");

		assertThat(result, hasItem("create view blahblah1 as select 1 from dual"));
		assertThat(result, hasItem("create table x as with basedata as (select 1 from dual) select * from basedata"));

		assertThat(result.size(), is(2));
	}

	@Test
	public void oracleGrant() throws Exception {
		List<String> result = oraSplitter.split("grant all on x to y;grant all on y to x;");

		assertThat(result, hasItem("grant all on x to y"));
		assertThat(result, hasItem("grant all on y to x"));

		assertThat(result.size(), is(2));
	}

	@Test
	public void oracleSlashAsDivisor() throws Exception {
		List<String> result = oraSplitter
				.split("select 1 as a from dual;select 1/2 as b from dual;select 1 / 2 as c from dual;");

		assertThat(result, hasItem("select 1 as a from dual"));
		assertThat(result, hasItem("select 1/2 as b from dual"));
		assertThat(result, hasItem("select 1 / 2 as c from dual"));

		assertThat(result.size(), is(3));
	}

	@Test
	public void oracleSlashAsText() throws Exception {
		List<String> result = oraSplitter
				.split("select 1 as a from dual;select to_date('31//12//1999','dd//mm//yyyy') from dual;");

		assertThat(result, hasItem("select 1 as a from dual"));
		assertThat(result, hasItem("select to_date('31//12//1999','dd//mm//yyyy') from dual"));

		assertThat(result.size(), is(2));
	}

	@Test
	public void oracleSlashAsBuffer() throws Exception {
		List<String> result = oraSplitter
				.split("select 1 as a from dual\n/;select to_date('31//12//1999','dd//mm//yyyy') from dual;");

		assertThat(result, hasItem("select 1 as a from dual"));
		assertThat(result, hasItem("select to_date('31//12//1999','dd//mm//yyyy') from dual"));

		assertThat(result.size(), is(2));
	}

	@Test
	public void oracleSlashAsSingleBuffer() throws Exception {
		List<String> result = oraSplitter.split("select 1 as a from dual;\n/");

		assertThat(result, hasItem("select 1 as a from dual"));
		assertThat(result.size(), is(1));
	}

	@Test
	public void oracleSlashAsOrphanedBuffer1() throws Exception {
		List<String> result = oraSplitter.split("\n;/\nselect 1 as a from dual;");

		assertThat(result, hasItem("select 1 as a from dual"));
		assertThat(result.size(), is(1));
	}

	@Test
	public void oracleSlashAsOrphanedBuffer2() throws Exception {
		List<String> result = oraSplitter
				.split("/" + System.getProperty("line.separator") + "select 1 as a from dual;");

		assertThat(result, hasItem("select 1 as a from dual"));
		assertThat(result.size(), is(1));
	}
	
	@Test
	public void oracleCase() throws Exception {
		List<String> result = oraSplitter
				.split("/" + System.getProperty("line.separator") + "select case when 1=2 then 0 else 1 end as a from dual;");

		assertThat(result, hasItem("select case when 1=2 then 0 else 1 end as a from dual"));
		assertThat(result.size(), is(1));
	}
	
	@Test
	public void oracleCaseInFunction1() throws Exception {
		List<String> result = oraSplitter
				.split("CREATE FUNCTION as v_long varchar2(4000); BEGIN\nselect case when 1=2 then 0 else 1 end as a from dual;\nEND;");
		assertThat(result, hasItem("CREATE FUNCTION as v_long varchar2(4000); BEGIN select case when 1=2 then 0 else 1 end as a from dual; END;"));
		assertThat(result.size(), is(1));
	}
	
	@Test
	public void oracleCaseInFunction2() throws Exception {
		List<String> result = oraSplitter
				.split("create function return integer as begin return case when 1=2 then 0 else 1 end;end;");
		assertThat(result, hasItem("create function return integer as begin return case when 1=2 then 0 else 1 end; end;"));
		assertThat(result.size(), is(1));
	}
	
	@Test
	public void oracleCaseUpdate() throws Exception {
		List<String> result = oraSplitter
				.split("/" + System.getProperty("line.separator") + "update x set y = (case when 1=2 then 0 else 1 end);");

		assertThat(result, hasItem("update x set y = (case when 1=2 then 0 else 1 end)"));
		assertThat(result.size(), is(1));
	}

    @Test
    public void oracleDivisorWithGreatest() throws Exception {
        List<String> result = oraSplitter
                .split("select (1+2+3)/greatest(case when 1=2 then 1 else 2 end,2) as a from dual;");

        assertThat(result, hasItem("select (1+2+3)/greatest(case when 1=2 then 1 else 2 end,2) as a from dual"));
        assertThat(result.size(), is(1));
    }
}
