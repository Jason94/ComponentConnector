package connector.runners

import connector.controller._
import connector.controller.cass_writer._
import org.apache.spark._
import org.apache.spark.graphx.GraphXUtils
import akka.actor.{ActorSystem, Props}
import java.net.Socket

class IntCodeCassWriter(nNodes: Int, nKeyspaces: Int, cassIp: String) 
		extends CassandraWriter(nNodes, nKeyspaces, cassIp) {
	
	def keyspaceSuffix(s: UID) = s(0)
	
}

object IntCodeRunner extends App {
	
	/// Set up Spark ///
	val conf = new SparkConf()
		.setMaster("local[2]")
	GraphXUtils.registerKryoClasses(conf)
	val sc = new SparkContext(conf.setAppName("Int-Code-Spark-App"))
	
	/// Set up Akka ///
	val system = ActorSystem("Int-Code-Actors")
	
	/// Start the controller ///
	val socket = new Socket("10.0.0.0", 9999) //TODO: Fix the ip address
	val streamSource = new SocketStreamSource(socket)
	
	val cassWriter = new IntCodeCassWriter(1, 10, "10.104.251.1")
	val controller = system.actorOf(
			Props(new Controller(system, sc, cassWriter)), "int-code-controller")
			
	controller ! StartListening(streamSource)
}