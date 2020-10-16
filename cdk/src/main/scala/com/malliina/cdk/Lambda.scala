package com.malliina.cdk

import com.malliina.cdk.Lambda.LambdaConf
import software.amazon.awscdk.core.Stack
import software.amazon.awscdk.services.cloudformation.CloudFormationCapabilities
import software.amazon.awscdk.services.codebuild.{BuildEnvironment, BuildSpec, ComputeType, LinuxBuildImage, PipelineProject}
import software.amazon.awscdk.services.codecommit.Repository
import software.amazon.awscdk.services.codepipeline.actions.{CloudFormationCreateReplaceChangeSetAction, CloudFormationExecuteChangeSetAction, CodeBuildAction, CodeCommitSourceAction}
import software.amazon.awscdk.services.codepipeline.{Artifact, Pipeline}
import software.amazon.awscdk.services.iam.{ManagedPolicy, PolicyStatement, Role}
import software.amazon.awscdk.services.s3.Bucket
import software.constructs.Construct

object Lambda {
  case class LambdaConf(todo: String)
  def apply(conf: LambdaConf, scope: Construct, stackName: String): Lambda =
    new Lambda(conf, scope, stackName)
}

class Lambda(conf: LambdaConf, scope: Construct, stackName: String)
  extends Stack(scope, stackName, CDK.stackProps)
  with CDKSyntax {
  val stack = this
  val source = Repository.Builder.create(stack, "Source").repositoryName(getStackName).build()
  val functionsBucket = Bucket.Builder.create(stack, "Functions").build()
//  val buildRole = Role.Builder
//    .create(stack, "BuildRole")
//    .assumedBy(principal("codebuild.amazonaws.com"))
//    .managedPolicies(
//      list(
//        ManagedPolicy.fromAwsManagedPolicyName("CloudWatchLogsFullAccess")
//      )
//    )
//    .build()
  val build = PipelineProject.Builder
    .create(stack, "CodeBuild")
    .projectName(getStackName)
    //    .role(buildRole)
    .environment(
      BuildEnvironment
        .builder()
        .buildImage(LinuxBuildImage.STANDARD_2_0)
        .computeType(ComputeType.MEDIUM)
        .build()
    )
    .environmentVariables(
      map(
        "BUCKET_NAME" -> buildEnv(functionsBucket.getBucketName)
      )
    )
    .buildSpec(BuildSpec.fromSourceFilename("lambda/scala/buildspec.yml"))
    .build()
  val pipelineRole = Role.Builder
    .create(stack, "CodePipelineRole")
    .assumedBy(principal("codepipeline.amazonaws.com"))
    .managedPolicies(
      list(
        //        ManagedPolicy.fromAwsManagedPolicyName("AmazonS3FullAccess")
      )
    )
    .build()
  functionsBucket.grantReadWrite(pipelineRole)
  val sourceOut = new Artifact()
  val buildOut = new Artifact()
  val changeSetName = "LambdaChangeSet"
  val lambdaStackName = s"$getStackName-lambda"
  val pipeline = Pipeline.Builder
    .create(stack, "Pipeline")
    .pipelineName(getStackName)
    .stages(
      list(
        stage("Source")(
          CodeCommitSourceAction.Builder
            .create()
            .actionName("SourceAction")
            .repository(source)
            .branch("master")
            .output(sourceOut)
            .build()
        ),
        stage("Build")(
          CodeBuildAction.Builder
            .create()
            .actionName("BuildAction")
            .project(build)
            .input(sourceOut)
            .outputs(list(buildOut))
            .build()
        ),
        stage("Stage")(
          CloudFormationCreateReplaceChangeSetAction.Builder
            .create()
            .actionName("StageAction")
            .changeSetName(changeSetName)
            .templatePath(buildOut.atPath("output.cfn.yml"))
            .stackName(lambdaStackName)
            .adminPermissions(true)
            .capabilities(
              list(
                CloudFormationCapabilities.ANONYMOUS_IAM,
                CloudFormationCapabilities.NAMED_IAM,
                CloudFormationCapabilities.AUTO_EXPAND
              )
            )
            .build()
        ),
        stage("Deploy")(
          CloudFormationExecuteChangeSetAction.Builder
            .create()
            .actionName("DeployAction")
            .changeSetName(changeSetName)
            .stackName(lambdaStackName)
            .build()
        )
      )
    )
    .build()
}
