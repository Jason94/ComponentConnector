package connector.test.integration

import org.scalatest.{FlatSpec, MustMatchers}
import org.apache.spark._
import org.apache.spark.graphx._
import akka.actor._
import java.net.Socket
import connector.controller._
import java.io.BufferedReader
import java.io.PrintWriter
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._

class MockComponentsWriter extends ComponentsWriter {
	var ccs: Option[VertexRDD[VertexId]] = None
	
	def writeCCs(vertices: VertexRDD[VertexId]) = {
		ccs = Some(vertices)
		sendFinished
	}
}

/** 
 *  Test the entire stack except the Cassandra database writer, which is mocked.
 */
class SansData extends FlatSpec with MustMatchers with MockitoSugar {
	
	"The stack" should "receive edge data and write the new cc information for 1 iteration" in {
		/// Test graph:
		// 1-3, 3-5, 3-6, 5-6
		// 2-4, 4-7, 7-10
		// 9-8
		
		/// Initialize the spark & akka contexts.
		val conf = new SparkConf().setMaster("local[1]")
		val sc = new SparkContext(conf.setAppName("StackSansDataTest"))
		
		val akkaSystem = ActorSystem("Stack-Sans-Data-Test")
		
		/// Set up the mocks.
		val streamMock = mock[ControllerStreamSource]
		val readerMock = mock[BufferedReader]
		val writerMock = mock[PrintWriter]
		
		when(streamMock.inputStream).thenReturn(readerMock)
		when(streamMock.outputStream).thenReturn(writerMock)
		
		// 1. Start the controller.
		// 2. Send some data in different lines.
		// 3. Finished mapping.
		// 4. Send the test exit signal, which instructs the controller to stop listening
		//    to the input stream so that it doesn't read 'c2' forever.
		when(readerMock.readLine).thenReturn(
			"c1",
			"1 3;3 6;9 8",
			"3 5;2 4",
			"5 6",
			"4 7;7 10",
			"c2",
			"cTEST_EXIT"
		)
		
		val ccWriter = new MockComponentsWriter
		
		/// Wait a bit, to make sure that the systems are all initialized.
		println("waiting for spark to init...")
		Thread.sleep(2000)
		
		/// Create the controller & send!
		val controller = akkaSystem.actorOf(Props(new Controller(akkaSystem, sc, ccWriter)))
		
		controller ! StartListening(streamMock)
		
		// Wait a bit, just to make sure...
		println("waitig for controller...")
		Thread.sleep(2000)
		
		// Now shutdown the controller.
		controller ! Shutdown
		
		/// Now verify!
		// The controller should respond to the stream source.
		verify(writerMock).print("c3")
		
		val ccs = ccWriter.ccs.get.collect.foldRight(Map[Long, Long]()) { (tup, mp) => mp + tup}
		
		ccs.keys.toList must have length 10
		
		// Make sure that ccs equate correctly.
		// Test graph:
		// 1-3, 3-5, 3-6, 5-6
		// 2-4, 4-7, 7-10
		// 9-8
		ccs(1) mustEqual ccs(3)
		ccs(1) mustEqual ccs(5)
		ccs(1) mustEqual ccs(6)
		ccs(2) mustEqual ccs(4)
		ccs(2) mustEqual ccs(7)
		ccs(2) mustEqual ccs(10)
		ccs(8) mustEqual ccs(9)
		
		ccs(1) mustNot equal(ccs(2))
		ccs(1) mustNot equal(ccs(8))
		ccs(2) mustNot equal(ccs(8))
	}
}