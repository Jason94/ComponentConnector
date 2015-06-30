package connector.controller

import akka.actor._
import org.apache.spark.graphx._
import org.apache.spark._
import scala.io.Source
import java.net.Socket
import java.io.{BufferedReader, PrintWriter, InputStreamReader}
import connector.logger.Logger
import connector.controller.stream_source.ControllerStreamSource
import scala.concurrent.stm._
import connector.controller.stream_source.StartListening

sealed abstract trait ControllerMessage
case class StartController(streamSource: ControllerStreamSource) extends ControllerMessage
case class ReceivePyMessage(msg: PyScMessage) extends ControllerMessage
case class ReadFromFile(fname: String) extends ControllerMessage
case class SendGraph(g: Graph[Nothing, Nothing]) extends ControllerMessage
case object PrintComps extends 	ControllerMessage
case object ShutdownController extends ControllerMessage
case object FinishedWritingCCs extends ControllerMessage

/** An Akka actor that communicates with the Python side of the framework and directs
 *  the GraphX calculations on the Scala side. Uses a TCP/IP socket to communicate with
 *  Python and the Akka Actor system to communicate with other Scala objects.
 *  Receives messages that inherit from [[connector.controller.ControllerMessage]].
 *  
 *  @constructor Use a given ActorSystem & SparkContext.
 *  @param actSys The actor system
 *  @param sc The spark context 
 */
class Controller(actSys: ActorSystem, sc: SparkContext, ccWriter: ComponentsWriter) extends Actor {
	private var graph: Option[Graph[Nothing, Nothing]] = None
	private var edgeBuilder = 
		actSys.actorOf(Props(new EdgeRddBuilder(sc)), "edgebuilder")
	private var streamSource: Option[ControllerStreamSource] = None
	private var inputReceiver: Option[ActorRef] = None
	private val inputReceiverStore = Ref(false)
	private var out: Option[PrintWriter] = None
	
	private var stopListening = false
	
	private def receiveMessage(message: PyScMessage) {
		Logger(s"Received: $message")
		
		// React.
		message match {
			case ListenForEdges => {
				// Reset the edge builder.
				edgeBuilder ! Reset
			} case FinishedMapping => {
				// Retrieve the graph from the edge builder.
				// Control will resume when the edgeBuilder sends
				// "SendGraph" back to us.
				Logger("sending graph request")
				edgeBuilder ! RequestGraph
			} case Shutdown => {
				stopListening = true
				self ! ShutdownController
			} case DataMessage(edges) => {
				edgeBuilder ! AddEdges(edges)
			}
		}
	}
	
	/*** Implement the messages ***/
	private def startListening(newStreamSource: ControllerStreamSource) {
		// Store the socket.
		streamSource = Some(newStreamSource)
		
		// Prepare to listen.		
		inputReceiver = Some(actSys.actorOf(
				Props(streamSource.get.inputStreamFactory(inputReceiverStore, self)), "input-receiver"))
		out = Some(streamSource.get.outputStream)
		
		// Listen
		inputReceiver.get ! StartListening
	}
	private def buildGraphFromFile(fname: String) {
		val edgeBuilder = actSys.actorOf(Props(new EdgeRddBuilder(sc)), "edgebuilder")
		for(lines <- Source.fromFile(fname).getLines()) {
			val ids = lines.split(" ")
			val edge = Edge(ids(0).toInt, ids(1).toInt)
			edgeBuilder ! AddEdge(edge)
		}
		edgeBuilder ! RequestGraph
	}
	private def sentGraph(g: Graph[Nothing, Nothing]) {
		// Store the graph.
		graph = Some(g)
		// Give the ccWriter a reference to our actorRef, so that it can send a 'FinishedWritingCCs'
		// message back to us.
		ccWriter.setController(self)
		// Now dispatch the cc's to the writer to get written to the database or whatever.
		// TODO: Fix the terrible type casting?
		ccWriter.writeCCs(graph.get.asInstanceOf[Graph[Int,Int]].connectedComponents.vertices)
		// Control will resume when the ccWriter sends a 'FinishedWritingCCs' message back to us.
	}
	private def printComps {
		// TODO: Is there a better way to get the CC's of a graph of nothing's? The type casting is too hacky.
		val cc = graph.get.asInstanceOf[Graph[Int, Int]].connectedComponents.vertices
		Logger(cc.collect().mkString("\n"))
	}
	private def shutdown {
		// Shutdown the akka system, spark context, and close the stream source.
		actSys.shutdown
		sc.stop
		streamSource match {
			case Some(src) => src.close
			case None => // Do nothing
		}
	}
	private def sendCcFinishedMessage {
		// If there's an open stream source, write a CCsWritten message back.
		out match {
			case Some(writer) => writer.print(CCsWritten.toString)
			case None => // Do nothing
		}
	}
	
	def receive = {
		case StartController(newStreamSource) => startListening(newStreamSource)
		case ReadFromFile(fname) => buildGraphFromFile(fname)
		case SendGraph(g) => sentGraph(g)
		case PrintComps => printComps
		case ShutdownController => shutdown
		case FinishedWritingCCs => sendCcFinishedMessage
	}
}