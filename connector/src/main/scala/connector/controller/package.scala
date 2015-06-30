package connector

import java.net.Socket
import connector.controller.stream_source.SocketStreamSource

package object controller {
	
	type UID = String
	
	implicit def socket2ControllerStreamSource(sock: Socket) = new SocketStreamSource(sock)
	
}