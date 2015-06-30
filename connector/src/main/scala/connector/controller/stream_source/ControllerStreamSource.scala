package connector.controller.stream_source

import scala.concurrent.stm._
import java.io.{BufferedReader, PrintWriter, InputStreamReader}
import java.net.Socket
import akka.actor.Actor
import akka.actor.ActorRef

/**
 * For testing, flexibility, & threading purposes, extract reading from a Socket directly
 * from the controller. 
 * 
 * Actors implementing this protocol should send a [[connector.controller.ReceivePyMessage]]
 * message to the controller whenever they receive a new message.
 */
sealed abstract class StreamReaderMessage
case object StartListening extends StreamReaderMessage

/**
 * For testing and complexity purposes, refactor extracting a BufferedReader & PrintWriter from
 * the Socket instance from the Controller class. The package object for [[connector.controller]]
 * will contain an implicit, socket2ControllerStreamSource to wrap a socket in an instance of this
 * trait.
 */
trait ControllerStreamSource {

	val inputStreamFactory: (Ref[Boolean], ActorRef) => ControllerInputSource
	val outputStream: PrintWriter
	
	private val inputCloseStore = Ref(false)
	
	/**
	 * Doesn't need to be overriden implemented in all cases, but useful to close a socket connection.
	 * In addition, closes the output & input streams.
	 */
	def close: Unit = {
		atomic { implicit txn => inputCloseStore() = true }
		outputStream.close
	}
}

abstract class ControllerInputSource(val shutdownStore: Ref[Boolean], val controller: ActorRef) extends Actor

class SocketStreamSource(sock: Socket) extends ControllerStreamSource {
	val inputStreamFactory = (store: Ref[Boolean], controller: ActorRef) => new SocketParser(sock.getInputStream, controller, store)
	val outputStream = new PrintWriter(sock.getOutputStream)
	
	override def close = {
		super.close
		sock.close
	}
}