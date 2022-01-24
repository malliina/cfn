scalaVersion := "2.12.15"

Seq(
  "com.eed3si9n" % "sbt-buildinfo" % "0.10.0",
  "org.scalameta" % "sbt-scalafmt" % "2.4.6",
  "com.eed3si9n" % "sbt-assembly" % "1.1.0"
) map addSbtPlugin
