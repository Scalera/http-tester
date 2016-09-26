name := "http-tester"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= {
  val akkaV = "2.4.4"
  Seq(
    "com.typesafe.akka" %% "akka-http-experimental" % akkaV)
}
