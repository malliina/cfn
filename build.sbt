import scala.sys.process.Process
import scala.util.Try

inThisBuild(
  Seq(
    scalaVersion := "3.6.2",
    organization := "com.malliina",
    version := "0.0.1",
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % "1.1.0" % Test
    )
  )
)

val isProd = settingKey[Boolean]("true if prod, false otherwise")

val versions = new {
  val logback = "1.5.16"
  val scalatags = "0.13.1"
}

val app = project
  .in(file("app"))
  .enablePlugins(BuildInfoPlugin, LiveRevolverPlugin)
  .settings(
    isProd := false,
    libraryDependencies ++= Seq("ember-server", "dsl", "circe").map { m =>
      "org.http4s" %% s"http4s-$m" % "0.23.30"
    } ++ Seq("classic", "core").map { m =>
      "ch.qos.logback" % s"logback-$m" % versions.logback
    } ++ Seq(
      "com.malliina" %% "database" % "6.9.8",
      "mysql" % "mysql-connector-java" % "8.0.33",
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

val opensearch = project
  .in(file("opensearch"))
  .settings(
    libraryDependencies ++= Seq(
      "com.amazonaws" % "aws-lambda-java-core" % "1.2.3",
      "com.amazonaws" % "aws-lambda-java-events" % "3.15.0"
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

val cdkVersion = "2.179.0"

val cdk = project
  .in(file("cdk"))
  .settings(
    libraryDependencies ++= Seq(
      "software.amazon.awscdk" % "aws-cdk-lib" % cdkVersion,
      "software.amazon.awscdk" % "amplify-alpha" % s"$cdkVersion-alpha.0"
    )
  )

val root = project.in(file(".")).aggregate(app, opensearch, website, cdk)

Global / onChangedBuildSource := ReloadOnSourceChanges

def gitHash: String =
  sys.env
    .get("GITHUB_SHA")
    .orElse(Try(Process("git rev-parse HEAD").lineStream.head).toOption)
    .getOrElse("unknown")
