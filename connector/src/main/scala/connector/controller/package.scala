package connector

import java.net.Socket

package object controller {
	
	type UID = String
	
	implicit def socket2ControllerStreamSource(sock: Socket) = new SocketStreamSource(sock)
	
}