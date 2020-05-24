name := "SChat"

version := "1.0"

lazy val scalaV = "2.13.1"
lazy val akkaHttpV = "10.1.12"
lazy val akkaV = "2.6.5"

lazy val osName = System.getProperty("os.name") match {
  case n if n.startsWith("Linux") => "linux"
  case n if n.startsWith("Mac") => "mac"
  case n if n.startsWith("Windows") => "win"
  case _ => throw new Exception("Unknown platform!")
}

lazy val javaFXModules = Seq("base", "controls", "fxml", "graphics", "media", "swing", "web")

resolvers += Resolver.sonatypeRepo("snapshots")

lazy val root =
  project.in(file("."))
    .aggregate(server, client)

lazy val server =
  project.in(file("server"))
    .settings(
      scalaVersion := scalaV,
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-actor-typed" % akkaV,
        "com.typesafe.akka" %% "akka-stream" % akkaV,
        "com.typesafe.akka" %% "akka-http" % akkaHttpV,
        "ch.qos.logback" % "logback-classic" % "1.2.3"
      )
    )

lazy val client =
  project.in(file("client"))
    .settings(
      scalaVersion := scalaV,
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-stream" % akkaV,
        "com.typesafe.akka" %% "akka-http" % akkaHttpV,
        "com.typesafe.akka" %% "akka-http-core" % akkaHttpV,
        "ch.qos.logback" % "logback-classic" % "1.2.3",
        "org.scalafx" %% "scalafx" % "14-R19"
      ),
      libraryDependencies ++= javaFXModules.map( m=>
        "org.openjfx" % s"javafx-$m" % "14.0.1" classifier osName
      )
    )

