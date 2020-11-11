scalaVersion := "2.12.12"

scalacOptions ++= Seq("-unchecked", "-deprecation")

Seq(
  "com.eed3si9n" % "sbt-buildinfo" % "0.10.0",
  "org.scalameta" % "sbt-scalafmt" % "2.4.2",
  "ch.epfl.scala" % "sbt-bloop" % "1.4.5"
) map addSbtPlugin
