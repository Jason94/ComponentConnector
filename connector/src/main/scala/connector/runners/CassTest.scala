package connector.runners

import scala.collection.JavaConversions._
import com.datastax.driver.core._
import com.datastax.driver.core.querybuilder.QueryBuilder
import akka.actor._

case class WriteToDatabase(a: String, b: Int)
class TestWriter(session: Session, writerId: Int) extends Actor {
	def receive = {
		case WriteToDatabase(a,b) => {
			val query = QueryBuilder.insertInto("test_keyspace", "test_table")
							.value("a", a)
							.value("b", b)
			session.execute(query)
		}
	}
}

case object Start
case class AlertFinished(id: Int)
class Coordinator(nWriters: Int) extends Actor {
	private var receives = Map(List.tabulate(nWriters)((_,false)):_*)
	
	def receive = {
		case Start => {
			// Connect to the cluster
			val cluster = Cluster.builder()
					.addContactPoint("10.104.251.45")
					.build
					
			val session = cluster.connect
			
			// Create the Akka system
			val n = 1
			
			val system = ActorSystem("cass-test")
			val actors = List.tabulate(n) { i =>
				system.actorOf(Props(new TestWriter(session, i)), s"writer-$i")
			}
			
			// Print to the table
			for(i <- 0 to 10) {
				actors(i % n) ! WriteToDatabase(s"entry $i", i)
			}
			
			Thread.sleep(1000)
		
			// Now look at the results
			val query = QueryBuilder.select.all.from("test_keyspace", "test_table")
			val results = session.execute(query)
			
			for(r <- results.all()) {
				println(r.toString())
			}
			
			session.close
			cluster.close
			system.shutdown
		}
	}
}

object CassTest extends App {
	
}