name := """reactive-benchmark"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.2"

resolvers ++= Seq(
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
  "rediscala" at "http://dl.bintray.com/etaty/maven"
)

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws,
  "mysql" % "mysql-connector-java" % "5.1.34",
  "com.etaty.rediscala" %% "rediscala" % "1.4.2",
  "org.mongodb" %% "casbah" % "2.8.0", //29-Jan-2015
  "org.reactivemongo" %% "play2-reactivemongo" % "0.10.5.0.akka23", //09-Oct-2014
  "org.webjars" %% "webjars-play" % "2.3.0-2",
  "org.webjars" % "jquery" % "2.1.3",
  "org.webjars" % "bootstrap" % "3.3.2"
)
