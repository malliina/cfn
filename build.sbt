val cdkModules =
  Seq("s3", "elasticbeanstalk", "codebuild", "codecommit", "codepipeline-actions", "lambda")

inThisBuild(
  Seq(
    scalaVersion := "2.13.3"
  )
)

val cdk = project
  .in(file("cdk"))
  .settings(
    libraryDependencies ++= cdkModules.map { module =>
      "software.amazon.awscdk" % module % "1.67.0"
    } ++ Seq(
      "org.scalameta" %% "munit" % "0.7.13" % Test
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )
