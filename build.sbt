import scala.sys.process.Process
import scala.util.Try

inThisBuild(
  Seq(
    scalaVersion := "3.2.2",
    organization := "com.malliina",
    version := "0.0.1",
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % "0.7.29" % Test
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )
)

val isProd = settingKey[Boolean]("true if prod, false otherwise")

val app = project
  .in(file("app"))
  .enablePlugins(BuildInfoPlugin, LiveRevolverPlugin)
  .settings(
    isProd := false,
    libraryDependencies ++= Seq("ember-server", "dsl", "circe").map { m =>
      "org.http4s" %% s"http4s-$m" % "0.23.18"
    } ++ Seq("classic", "core").map { m =>
      "ch.qos.logback" % s"logback-$m" % "1.4.5"
    } ++ Seq(
      "com.lihaoyi" %% "scalatags" % "0.12.0"
    ),
    buildInfoPackage := "com.malliina.app",
    buildInfoKeys := Seq[BuildInfoKey]("isProd" -> isProd.value),
    assemblyMergeStrategy := {
      case PathList("module-info.class") => MergeStrategy.discard
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
      "com.amazonaws" % "aws-lambda-java-core" % "1.2.2",
      "com.amazonaws" % "aws-lambda-java-events" % "3.11.0"
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
      Seq("classic", "core").map { m => "ch.qos.logback" % s"logback-$m" % "1.4.5" } ++
        Seq("com.lihaoyi" %% "scalatags" % "0.12.0"),
    buildInfoPackage := "com.malliina.website",
    buildInfoKeys := Seq[BuildInfoKey](
      name,
      version,
      scalaVersion,
      "gitHash" -> gitHash
    )
  )

val cdkVersion = "2.65.0"

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
