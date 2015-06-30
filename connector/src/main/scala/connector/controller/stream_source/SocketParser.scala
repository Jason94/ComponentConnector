package connector.controller.stream_source

import akka.actor.{Actor, ActorRef}
import java.io.{InputStream, BufferedReader, InputStreamReader}
import scala.concurrent.stm._
import connector.controller.PyScMessage
import connector.controller.ReceivePyMessage

/**
 * Reads from an input stream (usually from a network socket), delineates messages
 * according to the defined protocol, and outputs them in a new input stream.
 * 
 * @param inputStream InputStream to read from.
 * @param shutdownStore Alert the parser actor that it should shutdown early.
 */
class SocketParser(inputStream: InputStream, controller: ActorRef, shutdownStore: Ref[Boolean]) 
		extends ControllerInputSource(shutdownStore, controller) {
	
	private val reader = new BufferedReader(new InputStreamReader(inputStream))
	
	private def listen {
		var buffer = ""
		var input = ""
		//TODO: Apparently this does nothing in scala?
		while((input = reader.readLine()) != null && !shutdownStore.single()) {
			if(!input.contains("#"))
				buffer += input
			else {
				//TODO: Handle multiple '#' in one packet
				val splits = input.split("#")
				buffer += splits(0)
				val message = PyScMessage.parse(buffer)
				buffer = splits(1)
				
				controller ! ReceivePyMessage(message)
			}
		}
	}
	
	def receive = {
		case StartListening => listen
	}
}