package connector.runners

import org.apache.spark._
import org.apache.spark.graphx._
import org.apache.spark.graphx.impl._
import akka.actor._
import connector.controller.cass_writer._

object MyApp {
  def main(args: Array[String]) {

    /*
    // Load the graph
    val graph = GraphLoader.edgeListFile(sc, "data/followers.txt")
    // Find the cc's
    val cc = graph.connectedComponents().vertices
    // Join the cc's with usernames
    val users = sc.textFile("data/users.txt").map { line =>
      val fields = line.split(",")
      (fields(0).toLong, fields(1))
    }
    val ccByUsername = users.join(cc).map {
      case (id, (username, cc)) => (username, cc)
    }
    // Print the result
    println(ccByUsername.collect().mkString("\n"))
    // Stop the context
    sc.stop()
    */

    /*val emtpyEdges = Array[Edge[Int]](Edge(1,2), Edge(1,3), Edge(3, 4), Edge(6, 7))
    val emptyRdd = sc.parallelize(emtpyEdges)
    val edgeRdd = EdgeRDD.fromEdges(emptyRdd)
    val graph = Graph.fromEdges(edgeRdd, 0)
    val cc = graph.connectedComponents().vertices

    println(cc.collect().mkString("\n"))

    sc.stop()*/
	
    /// Set up spark ///
	// Create the spark context
	  /*
    val conf = new SparkConf()
      .setMaster("local[2]")
    GraphXUtils.registerKryoClasses(conf)
    val sc = new SparkContext(conf.setAppName("TestApp"))

    /// Set up Akka ///
    val system = ActorSystem("Edge-Test")
    
    /// Start the problem ///
    val dataFile = "data/followers.txt"
    
   	val controller = system.actorOf(Props(new Controller(system, sc, new CassandraWriter(1))), "controller")   
   	
   	controller ! ReadFromFile(dataFile)
   	
   	Thread.sleep(5000)
   	sc.stop()
   	system.shutdown()
   	* 
   	*/
  }
}
