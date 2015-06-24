package connector.controller.cass_writer

import connector.controller._
import org.apache.spark.graphx.{VertexRDD, VertexId}
import akka.actor.{ActorSystem, Props}

/**
 * Write connected component information to a specified Cassandra database.
 * Uses multiple actors running concurrently to scale synchronous writes
 * to the database on large clusters.
 * 
 * Dispatches to [[connector.controller.cass_writer.WriteActor]] to actually
 * write to the database.
 * 
 * Should be inherited and the method 'keyspaceSuffix' should be overwritten,
 * based on the domain of the problem.
 * 
 * @param nNodes Number of nodes available for the write system to use.
 */
abstract class CassandraWriter(nNodes: Int, nKeyspaces: Int, cassIp: String, keyspacePrefix: String = "connector") extends ComponentsWriter {
	
	lazy val keyspaces = List.tabulate(nKeyspaces)(i => keyspacePrefix + s"_$i")
	private val writerSystem = ActorSystem("Cass-Write-System")
	
	def keyspaceSuffix(s: UID)
	
	def determineKeyspace(s: UID) = keyspacePrefix + "_" + keyspaceSuffix(s)
	
	def writeCCs(vertices: VertexRDD[VertexId]) {
		
		// Calculate the load per actor.
		val writesPerActor = vertices.count / nNodes
		
		// Crate the actors.
		val writers = List.tabulate(nNodes)(i => writerSystem.actorOf(
				Props(new WriteActor(i, determineKeyspace _, keyspaces)), s"Writer-$i"))
		
		writers.foreach(_ ! OpenSession(cassIp))
				
		// Dispatch the writes.
		val verticesCollected = vertices.collect() //TODO: Is collecting here too inefficient?
		for(i <- 0 until verticesCollected.length) {
			writers(i % nNodes) ! WriteCc(verticesCollected(i)._1, verticesCollected(i)._2)
		}				
	}	
}