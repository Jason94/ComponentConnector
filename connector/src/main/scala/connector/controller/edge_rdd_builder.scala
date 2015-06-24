package connector.controller

import akka.actor.{Actor, ActorRef}
import org.apache.spark._
import org.apache.spark.graphx._
import org.apache.spark.graphx.impl.GraphImpl
import connector.logger.Logger

sealed abstract trait EdgeRddBuilderMessage
case object Reset extends EdgeRddBuilderMessage
case class AddEdge(edge: Edge[Nothing]) extends EdgeRddBuilderMessage
case class AddEdges(edges: List[Edge[Nothing]]) extends EdgeRddBuilderMessage
case object RequestGraph extends EdgeRddBuilderMessage

/** An Akka actor that builds an EdgeRDD from edge objects.
 *  Should be instantiated using an ActorSystem, not by calling a constructor.
 *  Receives messages that inherit from [[connector.controller.EdgeRddBuilderMessage]].
 *  
 *  @constructor Build the RDD in a specific SparkContext.
 *  @param sc The spark context 
 */
class EdgeRddBuilder(sc: SparkContext) extends Actor {
	private var edges: EdgeRDD[Nothing] = newEdgeRDD

	/** Constructs an empty EdgeRDD. */
	private def newEdgeRDD() = EdgeRDD.fromEdges[Nothing, Int](sc.parallelize(Array[Edge[Nothing]]()))
	
	/*** Implement the messages ***/
	private def reset() { 
		edges = newEdgeRDD 
	}
	private def addEdge(edge: Edge[Nothing]) {
		Logger("Adding edge: " + edge.toString)
		val newRdd = EdgeRDD.fromEdges[Nothing, Nothing](sc.parallelize(List(edge)))
		edges = EdgeRDD.fromEdges[Nothing, Nothing](edges.union(newRdd))
	}
	private def requestGraph(sender: ActorRef) {
		//TODO: Is there any way to do fromEdges[Nothing]?
		val verts = VertexRDD.fromEdges[Int](edges, 1, 0).asInstanceOf[VertexRDD[Nothing]]
		val graph = GraphImpl.fromExistingRDDs[Nothing, Nothing](verts, edges)
		sender ! SendGraph(graph)
	}

	def receive = {
		case Reset => edges = newEdgeRDD
		case AddEdge(edge) => addEdge(edge)
		case AddEdges(edges) => edges foreach (addEdge(_))
		case RequestGraph => requestGraph(sender)
	}
}
