inThisBuild(
  Seq(
    scalaVersion := "3.1.1",
    organization := "com.malliina",
    version := "0.0.1",
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % "0.7.29" % Test
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )
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
  .settings(
    libraryDependencies ++=
      Seq("classic", "core").map { m => "ch.qos.logback" % s"logback-$m" % "1.4.5" } ++
        Seq("com.lihaoyi" %% "scalatags" % "0.12.0")
  )

val cdkVersion = "2.58.1"

val cdk = project
  .in(file("cdk"))
  .settings(
    libraryDependencies ++= Seq(
      "software.amazon.awscdk" % "aws-cdk-lib" % cdkVersion,
      "software.amazon.awscdk" % "amplify-alpha" % s"$cdkVersion-alpha.0"
    )
  )

val root = project.in(file(".")).aggregate(opensearch, website, cdk)

Global / onChangedBuildSource := ReloadOnSourceChanges
