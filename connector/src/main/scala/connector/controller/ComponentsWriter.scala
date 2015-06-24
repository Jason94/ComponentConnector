package connector.controller

import org.apache.spark.graphx.{VertexRDD, VertexId}
import akka.actor.ActorRef

/**
 * For testing and complexity purposes, extract writing the CC's to the
 * Cassandra database to a trait.
 * 
 * When the ComponentsWriter is finished writing, it should call the sendFinished
 * method, which sends a message to the controller that all of the CC information
 * has been written.
 */
abstract class ComponentsWriter {
	
	private var controller: Option[ActorRef] = None

	def writeCCs(vertices: VertexRDD[VertexId])
	
	def setController(cntrlActor: ActorRef) = controller = Some(cntrlActor)
	
	def sendFinished() = controller match {
		case Some(cntrlActor) => cntrlActor ! FinishedWritingCCs
		case None => // Do nothing
	}	
}