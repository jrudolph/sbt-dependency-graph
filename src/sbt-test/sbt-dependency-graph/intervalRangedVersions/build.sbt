scalaVersion := "2.9.1"

resolvers += "typesafe maven" at "https://repo.typesafe.com/typesafe/maven-releases/"

libraryDependencies ++= Seq(
  "com.codahale" % "jerkson_2.9.1" % "0.5.0"
)

InputKey[Unit]("check") := {
  val report = (ivyReport in Test).value
  val graph = (asciiTree in Test).evaluated

  def sanitize(str: String): String = str.split('\n').drop(1).map(_.trim).mkString("\n")
  val expectedGraph =
    """default:default-dbc48d_2.9.2:0.1-SNAPSHOT [S]
      |  +-com.codahale:jerkson_2.9.1:0.5.0 [S]
      |    +-org.codehaus.jackson:jackson-core-asl:1.9.11
      |    +-org.codehaus.jackson:jackson-mapper-asl:1.9.11
      |      +-org.codehaus.jackson:jackson-core-asl:1.9.11
      |  """.stripMargin
  IO.writeLines(file("/tmp/blib"), sanitize(graph).split("\n"))
  IO.writeLines(file("/tmp/blub"), sanitize(expectedGraph).split("\n"))
  require(sanitize(graph) == sanitize(expectedGraph), "Graph for report %s was '\n%s' but should have been '\n%s'" format (report, sanitize(graph), sanitize(expectedGraph)))
  ()
}
