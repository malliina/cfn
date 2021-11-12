scalaVersion := "2.12.15"

scalacOptions ++= Seq("-unchecked", "-deprecation")

Seq(
  "com.eed3si9n" % "sbt-buildinfo" % "0.10.0",
  "org.scalameta" % "sbt-scalafmt" % "2.4.3"
) map addSbtPlugin
