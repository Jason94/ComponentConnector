lazy val commonSettings = Seq(
	scalaVersion := "2.11.6"
)

lazy val root = (project in file(".")).
	settings(commonSettings: _*).
	settings(
		name := "connector",
		libraryDependencies += "com.datastax.cassandra" % "cassandra-driver-core" % "2.1.6",
		libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
		libraryDependencies += "org.mockito" % "mockito-core" % "2.0.14-beta",
		libraryDependencies += ("org.scala-stm" %% "scala-stm" % "0.7"),
		mainClass := Some("MyApp")
	)


