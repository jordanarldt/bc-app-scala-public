lazy val akkaHttpVersion = "10.2.9"
lazy val akkaVersion    = "2.6.19"

// Run in a separate JVM, to make sure sbt waits until all threads have
// finished before returning.
// If you want to keep the application running while executing other
// sbt tasks, consider https://github.com/spray/sbt-revolver/
fork := true

enablePlugins(JavaAppPackaging)
Compile / mainClass := Some("com.example.HttpServer")

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization    := "com.example",
      scalaVersion    := "2.13.8"
    )),
    name := "bc-sample-app-scala",
    libraryDependencies ++= Seq(
      "com.typesafe.akka"     %% "akka-http"                % akkaHttpVersion,
      "com.typesafe.akka"     %% "akka-http-spray-json"     % akkaHttpVersion,
      "com.typesafe.akka"     %% "akka-actor-typed"         % akkaVersion,
      "com.typesafe.akka"     %% "akka-stream"              % akkaVersion,
      "com.typesafe.slick"    %% "slick"                    % "3.3.3",
      "com.typesafe.slick"    %% "slick-hikaricp"           % "3.3.3",
      "com.github.jwt-scala"  %% "jwt-core"                 % "9.0.5",
      "org.postgresql"        % "postgresql"                % "42.3.3",
      "ch.qos.logback"        % "logback-classic"           % "1.2.3",

      "com.typesafe.akka"  %% "akka-http-testkit"        % akkaHttpVersion % Test,
      "com.typesafe.akka"  %% "akka-actor-testkit-typed" % akkaVersion     % Test,
      "org.scalatest"      %% "scalatest"                % "3.1.4"         % Test
    )
  )
