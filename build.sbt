inThisBuild(
  Seq(
    scalaVersion := "2.13.7",
    organization := "com.malliina",
    version := "0.0.1",
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % "0.7.29" % Test
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )
)

val website = project
  .in(file("website"))
  .settings(
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "scalatags" % "0.10.0"
    )
  )

val cdkModules = Seq(
  "amplify",
  "codebuild",
  "codecommit",
  "codepipeline-actions",
  "elasticbeanstalk",
  "elasticsearch",
  "lambda",
  "s3"
)

val cdk = project
  .in(file("cdk"))
  .settings(
    libraryDependencies ++= cdkModules.map { module =>
      "software.amazon.awscdk" % module % "1.132.0"
    }
  )

val root = project.in(file(".")).aggregate(website, cdk)

Global / onChangedBuildSource := ReloadOnSourceChanges
