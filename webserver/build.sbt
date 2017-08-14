name := "akka-hightload-scala"

version := "1.0"

scalaVersion := "2.12.2"

lazy val akkaVersion = "2.5.3"

libraryDependencies ++= Seq(
//  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
//  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % "10.0.9",
  "io.spray" %%  "spray-json" % "1.3.3",
  "com.typesafe.akka" %% "akka-http-testkit" % "10.0.9" % Test,
"org.scalatest" %% "scalatest" % "3.0.1" % "test"
)
