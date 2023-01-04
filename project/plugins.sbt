scalaVersion := "2.12.17"

Seq(
  "com.eed3si9n" % "sbt-buildinfo" % "0.11.0",
  "org.scalameta" % "sbt-scalafmt" % "2.5.0",
  "com.eed3si9n" % "sbt-assembly" % "2.1.0"
) map addSbtPlugin
