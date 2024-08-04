scalaVersion := "2.12.19"

Seq(
  "com.malliina" % "live-reload" % "0.6.0",
  "io.spray" % "sbt-revolver" % "0.10.0",
  "com.eed3si9n" % "sbt-buildinfo" % "0.12.0",
  "org.scalameta" % "sbt-scalafmt" % "2.5.2",
  "com.eed3si9n" % "sbt-assembly" % "2.2.0"
) map addSbtPlugin
