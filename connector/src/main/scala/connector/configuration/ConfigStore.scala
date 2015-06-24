package connector.configuration

import scala.concurrent.stm._

/**
 * Store the various configuration information in an STM.
 */
object ConfigStore {
	
	val cassandraConfiguration: Ref[Option[CassandraConfiguration]] = Ref(None)
	val controllerConfiguration: Ref[Option[ControllerConfiguration]] = Ref(None)

	/**
	 * Helper method to store a [[connector.configuration.CassandraConfiguration]] instance
	 * in the ref in a transaction.
	 */
	def storeCassandraConfiguration(config: CassandraConfiguration) = atomic { implicit txn =>
		cassandraConfiguration() = Some(config)	
	}
	
	/**
	 * Helper method to store a [[connector.configuration.ControllerConfiguration]] instance
	 * in the ref in a transaction.
	 */
	def storeControllerConfiguration(config: ControllerConfiguration) = atomic { implicit txn =>
		controllerConfiguration() = Some(config)	
	}
}