package connector.controller.cass_writer

import akka.actor.Actor
import org.apache.spark.graphx.{VertexId}
import connector.controller.UID
import com.datastax.driver.core._
import com.datastax.driver.core.querybuilder.QueryBuilder
import scala.collection.JavaConversions._

sealed abstract class WriteActorMessage
case class OpenSession(ip: String) extends WriteActorMessage
case object CloseSession extends WriteActorMessage
case class WriteCc(oldCc: VertexId, newCc: VertexId) extends WriteActorMessage

/**
 * Actor that handles writing to the database synchronously, in parallel.
 */
class WriteActor(writerId: Int, keyspaceFn: UID => String, keyspaces: List[String]) extends Actor {
	private var session: Option[Session] = None
  
	private def writeToDatabase(oldCc: VertexId, newCc: VertexId) {
		// First, query the database to get all of the UID's of the old CC.
		var rows: List[Row] = List()
		
		for(kspc <- keyspaces) {
			val ccQuery = QueryBuilder
							.select()
							.column("uid")
							.from(keyspaces(0), "cmptable")
							.where(QueryBuilder.eq("cmp", oldCc))
			rows = rows ++ session.get.execute(ccQuery).all()
		}
		
		// Now write the new CC to the UID's.
		val uids = rows.map(_.getString("cmp"))
		
		for(uid <- uids) {
			UpdateQuerier.updateQuery(
				session = session.get, 
				kspc = keyspaceFn(uid), 
				table = "cmptable",
				clmn = "cmp",
				newVal = newCc.toString(),
				id = "uid",
				matchVal = uid
			)
		}
	}
	
	def receive = {
		case WriteCc(oldCc, newCc) => writeToDatabase(oldCc, newCc)
	}
}