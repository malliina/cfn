import scala.sys.process.Process
import scala.util.Try

inThisBuild(
  Seq(
    scalaVersion := "3.7.1",
    organization := "com.malliina",
    version := "0.0.1",
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % "1.1.1" % Test
    )
  )
)

val isProd = settingKey[Boolean]("true if prod, false otherwise")

val versions = new {
  val database = "6.9.10"
  val http4s = "0.23.30"
  val javaCore = "1.3.0"
  val javaEvents = "3.16.0"
  val logback = "1.5.18"
  val mysql = "8.0.33"
  val scalatags = "0.13.1"
}

val app = project
  .in(file("app"))
  .enablePlugins(BuildInfoPlugin, LiveRevolverPlugin)
  .settings(
    isProd := false,
    libraryDependencies ++= Seq("ember-server", "dsl", "circe").map { m =>
      "org.http4s" %% s"http4s-$m" % versions.http4s
    } ++ Seq("classic", "core").map { m =>
      "ch.qos.logback" % s"logback-$m" % versions.logback
    } ++ Seq(
      "com.malliina" %% "database" % versions.database,
      "mysql" % "mysql-connector-java" % versions.mysql,
      "com.lihaoyi" %% "scalatags" % versions.scalatags
    ),
    buildInfoPackage := "com.malliina.app",
    buildInfoKeys := Seq[BuildInfoKey]("isProd" -> isProd.value, "gitHash" -> gitHash),
    assemblyMergeStrategy := {
      case PathList("META-INF", "versions", xs @ _*) => MergeStrategy.first
      case PathList("module-info.class")             => MergeStrategy.discard
      case x =>
        val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
        oldStrategy(x)
    },
    assembly / assemblyJarName := "app.jar"
  )

val simple = project
  .in(file("simple"))
  .settings(
    assembly / assemblyJarName := "simple.jar",
    assembly := {
      val name = (assembly / assemblyJarName).value
      val src = assembly.value
      val dest = target.value / name
      IO.copyFile(src, dest)
      streams.value.log.info(s"Copied '$src' to '$dest'.")
      dest
    }
  )

val opensearch = project
  .in(file("opensearch"))
  .settings(
    libraryDependencies ++= Seq(
      "com.amazonaws" % "aws-lambda-java-core" % versions.javaCore,
      "com.amazonaws" % "aws-lambda-java-events" % versions.javaEvents
    ),
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", _ @_*) => MergeStrategy.discard
      case _                           => MergeStrategy.first
    },
    assembly := {
      val src = assembly.value
      val dest = baseDirectory.value / "function.jar"
      IO.copyFile(src, dest)
      streams.value.log.info(s"Copied '$src' to '$dest'.")
      dest
    }
  )

val website = project
  .in(file("website"))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    libraryDependencies ++=
      Seq("classic", "core").map { m =>
        "ch.qos.logback" % s"logback-$m" % versions.logback
      } ++ Seq("com.lihaoyi" %% "scalatags" % versions.scalatags),
    buildInfoPackage := "com.malliina.website",
    buildInfoKeys := Seq[BuildInfoKey](
      name,
      version,
      scalaVersion,
      "gitHash" -> gitHash
    )
  )

val cdkVersion = "2.203.0"

val cdk = project
  .in(file("cdk"))
  .settings(
    libraryDependencies ++= Seq(
      "software.amazon.awscdk" % "aws-cdk-lib" % cdkVersion,
      "software.amazon.awscdk" % "amplify-alpha" % s"$cdkVersion-alpha.0"
    )
  )

val root = project.in(file(".")).aggregate(app, opensearch, website, cdk, simple)

Global / onChangedBuildSource := ReloadOnSourceChanges

def gitHash: String =
  sys.env
    .get("GITHUB_SHA")
    .orElse(Try(Process("git rev-parse HEAD").lineStream.head).toOption)
    .getOrElse("unknown")
