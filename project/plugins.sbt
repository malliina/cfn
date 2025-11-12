scalaVersion := "2.12.20"

Seq(
  "com.malliina" % "live-reload" % "0.6.0",
  "io.spray" % "sbt-revolver" % "0.10.0",
  "com.eed3si9n" % "sbt-buildinfo" % "0.13.1",
  "org.scalameta" % "sbt-scalafmt" % "2.5.4",
  "com.eed3si9n" % "sbt-assembly" % "2.3.1"
) map addSbtPlugin
