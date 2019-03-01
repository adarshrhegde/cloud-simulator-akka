name := "CloudSimulatorAkka"

version := "1.0"

scalaVersion := "2.12.8"

lazy val akkaVersion = "2.5.17"

libraryDependencies ++= Seq(
  //Akka Actors
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,

  //Logging with Actors
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",

  //Scala's wrapper of Type safe config
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "com.github.pureconfig" %% "pureconfig" % "0.10.0",

  //Testing
  "org.scalatest" %% "scalatest" % "3.0.5" % Test,
  "com.typesafe.akka" %% "akka-testkit" % "2.5.19" % Test
)

// following setting is used to get more detailed errors for pureconfig config load failures
scalacOptions += "-Xmacro-settings:materialize-derivations"