name := "CloudSimulatorAkka"

version := "1.0"

scalaVersion := "2.12.8"

lazy val akkaVersion = "2.5.17"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "com.github.pureconfig" %% "pureconfig" % "0.10.0",
  "org.scalatest" %% "scalatest" % "3.0.5" % Test,
  "com.typesafe.akka" %% "akka-testkit" % "2.5.19" % Test
)