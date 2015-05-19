name := """akka-sample-main-scala"""

version := "2.3.5"

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.5",
  "com.typesafe.akka" %% "akka-remote" % "2.3.5",
  "edu.arizona.sista" % "processors" % "3.3",
  "edu.arizona.sista" % "processors" % "3.3" classifier "models",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.3.1",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.3.1" classifier "models",
  "com.io7m.xom" % "xom" % "1.2.10",
  "joda-time" % "joda-time" % "2.1",
  "de.jollyday" % "jollyday" % "0.4.7",
  "com.googlecode.efficient-java-matrix-library" % "ejml" % "0.23",
  "junit" % "junit" % "4.10",
  "org.scalatest" % "scalatest_2.10" % "2.0.M6-SNAP17",
  "xom" % "xom" % "1.2.5",
  "com.googlecode.efficient-java-matrix-library" % "ejml" % "0.19",
  "ch.qos.logback" % "logback-classic" % "1.0.10",
  "nz.ac.waikato.cms.weka" % "weka-dev" % "3.7.10",
  "net.sf.jopt-simple" % "jopt-simple" % "4.5",
  "de.bwaldvogel" % "liblinear" % "1.94",
  "log4j" % "log4j" % "1.2.17",
  "nz.ac.waikato.cms.weka" % "weka-dev" % "3.7.12",
  "tw.edu.ntu.csie" % "libsvm" % "3.17"
)

