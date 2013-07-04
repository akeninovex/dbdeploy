package com.dbdeploy.database;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class QueryStatementSplitterOracleTest {
	private QueryStatementSplitter oraSplitter;

	@Before
	public void setUp() throws Exception {
		oraSplitter = new QueryStatementSplitterOracle();
	}

	@Test
	public void oracleNestedBlocks() throws Exception {
		List<String> result = oraSplitter
				.split(" select 0 from dual; begin \n select 1 from dual; \n begin select 2 from dual; end \n ; end;");
		assertThat(result, hasItem("select 0 from dual"));
		assertThat(result, hasItem("begin select 1 from dual; begin select 2 from dual; end; end"));
		assertThat(result.size(), is(2));
	}

	@Test
	public void oracleLeaveDots() throws Exception {
		List<String> result = oraSplitter.split("CREATE SEQUENCE cdb_meta.seq_global\n;");
		assertThat(result, hasItem("CREATE SEQUENCE cdb_meta.seq_global"));
		assertThat(result.size(), is(1));
	}

	@Test
	public void oracleIgnoreComments() throws Exception {
		List<String> result = oraSplitter.split("-- here is my comment \n CREATE SEQUENCE cdb_meta.seq_global\n;");
		assertThat(result, hasItem("CREATE SEQUENCE cdb_meta.seq_global"));
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
		List<String> result = oraSplitter
				.split("CREATE TRIGGER cdb_imp.tr_bi_imp_booking\n BEFORE INSERT ON cdb_imp.imp_booking \nFOR EACH ROW "
						+ "\nBEGIN\n:new.id := cdb_meta.seq_global.nextval;\nEND;");
		assertThat(result,
				hasItem("CREATE TRIGGER cdb_imp.tr_bi_imp_booking BEFORE INSERT ON cdb_imp.imp_booking FOR EACH ROW "
						+ "BEGIN :new.id := cdb_meta.seq_global.nextval; END"));

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
				.split("CREATE FUNCTION as v_long varchar2(4000); BEGIN\nselect 1 from dual;\nEND;");
		assertThat(result, hasItem("CREATE FUNCTION as v_long varchar2(4000); BEGIN select 1 from dual; END"));

		assertThat(result.size(), is(1));
	}

	@Test
	public void oracleFunctionNestedBlocks() throws Exception {
		List<String> result = oraSplitter
				.split("CREATE FUNCTION as v_long varchar2(4000);BEGIN select 1 from dual;BEGIN select 2 from dual;END;END;");
		assertThat(
				result,
				hasItem("CREATE FUNCTION as v_long varchar2(4000); BEGIN select 1 from dual; BEGIN select 2 from dual; END; END"));

		assertThat(result.size(), is(1));
	}

	@Test
	public void oracleFunctionWithTypes() throws Exception {
		List<String> result = oraSplitter
				.split("create or replace type cdb_report.length_metric as object (col varchar2(200));"
						+ "create or replace type x as table of y;/"
						+ "create or replace function x(a in varchar2, b in varchar2) return y as l_a := varchar2(10);l_b := varchar2(10);"
						+ "begin select 1 from dual; select 2 from dual;end;/");
		assertThat(
				result,
				hasItem("create or replace function x(a in varchar2, b in varchar2) return y as l_a := varchar2(10); l_b := varchar2(10); "
						+ "begin select 1 from dual; select 2 from dual; end"));
		assertThat(result, hasItem("create or replace type x as table of y"));
		assertThat(result, hasItem("create or replace type cdb_report.length_metric as object (col varchar2(200))"));

		assertThat(result.size(), is(3));
	}

	@Test
	public void oracleFunctionComplexWithTypes() throws Exception {
		List<String> result = oraSplitter
				.split("create or replace type cdb_report.length_metric as object (col varchar2(200));"
						+ "create or replace type x as table of y;/"
						+ "create or replace function get_column_metrics(p_user in varchar2, p_table in varchar2) return length_metric_table as "
						+ "l_data length_metric_table := length_metric_table();type col_tbl is table of all_tab_columns.column_name%type; l_col_names col_tbl;"
						+ "begin select 1 from dual; select 2 from dual;end;/");
		assertThat(result, hasItem("create or replace type x as table of y"));
		assertThat(result, hasItem("create or replace type cdb_report.length_metric as object (col varchar2(200))"));
		assertThat(
				result,
				hasItem("create or replace function get_column_metrics(p_user in varchar2, p_table in varchar2) return length_metric_table as "
						+ "l_data length_metric_table := length_metric_table(); type col_tbl is table of all_tab_columns.column_name%type; "
						+ "l_col_names col_tbl; begin select 1 from dual; select 2 from dual; end"));

		assertThat(result.size(), is(3));
	}

	@Test
	public void oracleIndexes() throws Exception {
		List<String> result = oraSplitter
				.split("CREATE table blahblah (v_long varchar2(4000));\nALTER TABLE cdb_dwh.f_revenue ADD CONSTRAINT pk_f_revenue PRIMARY KEY\n(\nid\n)USING INDEX;CREATE UNIQUE INDEX cdb_dwh.ix_f_revenue_awb on cdb_dwh.f_revenue(awb);");

		assertThat(result, hasItem("CREATE table blahblah (v_long varchar2(4000))"));
		assertThat(result,
				hasItem("ALTER TABLE cdb_dwh.f_revenue ADD CONSTRAINT pk_f_revenue PRIMARY KEY ( id )USING INDEX"));
		assertThat(result, hasItem("CREATE UNIQUE INDEX cdb_dwh.ix_f_revenue_awb on cdb_dwh.f_revenue(awb)"));

		assertThat(result.size(), is(3));
	}

	@Test
	public void oracleIndexesReverse() throws Exception {
		List<String> result = oraSplitter
				.split("CREATE table blahblah (v_long varchar2(4000));CREATE UNIQUE INDEX cdb_dwh.ix_f_revenue_awb on cdb_dwh.f_revenue(awb);\nALTER TABLE cdb_dwh.f_revenue ADD CONSTRAINT pk_f_revenue PRIMARY KEY\n(\nid\n)USING INDEX;");

		assertThat(result, hasItem("CREATE table blahblah (v_long varchar2(4000))"));
		assertThat(result,
				hasItem("ALTER TABLE cdb_dwh.f_revenue ADD CONSTRAINT pk_f_revenue PRIMARY KEY ( id )USING INDEX"));
		assertThat(result, hasItem("CREATE UNIQUE INDEX cdb_dwh.ix_f_revenue_awb on cdb_dwh.f_revenue(awb)"));

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
				hasItem("CREATE FUNCTION as v_long varchar2(4000); BEGIN select 1 from dual; BEGIN select 2 from dual; END; END"));

		assertThat(result.size(), is(2));
	}

	@Test
	public void oracleViewPlusReverse() throws Exception {
		List<String> result = oraSplitter
				.split("CREATE FUNCTION as v_long varchar2(4000);BEGIN select 1 from dual;BEGIN select 2 from dual;END;END;CREATE view blahblah1 as select 1 from dual;");

		assertThat(result, hasItem("CREATE view blahblah1 as select 1 from dual"));
		assertThat(
				result,
				hasItem("CREATE FUNCTION as v_long varchar2(4000); BEGIN select 1 from dual; BEGIN select 2 from dual; END; END"));

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
}
