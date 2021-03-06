name := "sp-connector-manager"

version := "0.1"

scalaVersion := "2.12.7"

libraryDependencies ++= Seq(
  "com.twitter" %% "finagle-http" % "20.12.0",
  "com.github.finagle" %% "finagle-oauth2" % "19.8.0",
  "org.scalatest" %% "scalatest" % "3.2.2" % "test",
  "org.skinny-framework" %% "skinny-http-client" % "2.3.7",
  "com.typesafe" % "config" % "1.3.3",
  "com.thesamet.scalapb" %% "scalapb-json4s" % "0.10.1",
//  "org.json4s" %% "json4s-native" % "3.2.11",
//  "org.json4s" %% "json4s-jackson" % "3.6.10",
  "org.sourcelab" % "kafka-connect-client" % "3.1.0"
)

resolvers ++= Seq(
  //  "Local Maven Repository" at "file://" + Path.userHome.absolutePath + "/.m2/repository",
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots"),
  Resolver.bintrayRepo("cakesolutions", "maven"),
  "Artima Maven Repository" at "http://repo.artima.com/releases",
  "confluent" at "https://packages.confluent.io/maven/",
  "jitpack" at "https://jitpack.io"
)

PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value / "scalapb"
)

// (optional) If you need scalapb/scalapb.proto or anything from
// google/protobuf/*.proto
libraryDependencies ++= Seq(
  "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"
)

mainClass in assembly := Some("com.massmutual.streaming.manager.ConnectorService")
assemblyJarName in assembly := name.value + ".jar"
assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs@_*) => MergeStrategy.discard
  //  case "application.conf" => MergeStrategy.concat
  case _ => MergeStrategy.first
}