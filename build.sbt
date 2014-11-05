name := """akka-sample-main-scala"""

version := "2.3.5"

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.5",
  "edu.arizona.sista" % "processors" % "3.3",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.3.1"
  )

