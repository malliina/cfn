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

val cdk = project
  .in(file("cdk"))
  .settings(
    libraryDependencies ++= cdkModules.map { module =>
      "software.amazon.awscdk" % module % "1.72.0"
    }
  )

val root = project.in(file(".")).aggregate(website, cdk)
