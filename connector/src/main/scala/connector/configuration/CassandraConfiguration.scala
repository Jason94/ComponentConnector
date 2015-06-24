package connector.configuration

import connector.controller.UID

/**
 * Store configuration for connecting to and interacting with a Cassandra database.
 * 
 * @param nKeyspaces The number of keyspaces being used to store the vertices.
 * @param cassandraIp The ip address to target.
 * @param keyspacePrefix The prefix of the keyspace names, as in "KEYSPACEPREFIX_N"
 * @param determineKeyspace Given a particular UID, return the index of the keyspace.
 */
case class CassandraConfiguration(
	val nKeyspaces: Int,
	val cassandraIp: String,
	val keyspacePrefix: String,
	val determineKeyspaceN: UID => Int
) {
	/**
	 * Given a particular UID, return the full keyspace name.
	 */
	val determineKeyspace: UID => String = (uid) => keyspacePrefix + "_" + determineKeyspaceN(uid).toString
}
