val cdkModules =
  Seq(
    "amplify",
    "codebuild",
    "codecommit",
    "codepipeline-actions",
    "elasticbeanstalk",
    "lambda",
    "s3"
  )

inThisBuild(
  Seq(
    scalaVersion := "2.13.3",
    organization := "com.malliina",
    version := "0.0.1"
  )
)

val website = project
  .in(file("website"))
  .settings(
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "scalatags" % "0.9.1",
      "org.scalameta" %% "munit" % "0.7.17" % Test
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )

val cdk = project
  .in(file("cdk"))
  .settings(
    libraryDependencies ++= cdkModules.map { module =>
      "software.amazon.awscdk" % module % "1.72.0"
    } ++ Seq(
      "org.scalameta" %% "munit" % "0.7.17" % Test
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )

val root = project.in(file(".")).aggregate(website, cdk)
