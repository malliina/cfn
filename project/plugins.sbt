scalaVersion := "2.12.17"

Seq(
  "com.malliina" % "live-reload" % "0.5.0",
  "io.spray" % "sbt-revolver" % "0.9.1",
  "com.eed3si9n" % "sbt-buildinfo" % "0.11.0",
  "org.scalameta" % "sbt-scalafmt" % "2.5.0",
  "com.eed3si9n" % "sbt-assembly" % "2.1.1"
) map addSbtPlugin
