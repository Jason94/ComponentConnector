package connector.controller.cass_write;

import com.datastax.driver.core.*;
import com.datastax.driver.core.querybuilder.*;

public class UpdateQuerier {
	
	public static void updateQuery(Session session, String kspc, String table, String clmn, String newVal, String id, String matchVal) {
		
		Statement query = QueryBuilder
							.update(kspc, table)
							.with(QueryBuilder.set(clmn, newVal))
							.where(QueryBuilder.eq(id, matchVal));
		
		session.execute(query);
	}
}
