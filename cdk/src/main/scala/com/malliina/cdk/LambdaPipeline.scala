package com.malliina.cdk

import com.malliina.cdk.LambdaPipeline.LambdaConf
import software.amazon.awscdk.core.Stack
import software.amazon.awscdk.services.cloudformation.CloudFormationCapabilities
import software.amazon.awscdk.services.codebuild._
import software.amazon.awscdk.services.codecommit.Repository
import software.amazon.awscdk.services.codepipeline.actions.{CloudFormationCreateReplaceChangeSetAction, CloudFormationExecuteChangeSetAction, CodeBuildAction, CodeCommitSourceAction}
import software.amazon.awscdk.services.codepipeline.{Artifact, Pipeline}
import software.amazon.awscdk.services.lambda.CfnParametersCode
import software.constructs.Construct

object LambdaPipeline {
  case class LambdaConf(stackId: String, code: CfnParametersCode)

  def apply(conf: LambdaConf, scope: Construct, stackName: String): LambdaPipeline =
    new LambdaPipeline(conf, scope, stackName)
}

class LambdaPipeline(conf: LambdaConf, scope: Construct, stackName: String)
  extends Stack(scope, stackName, CDK.stackProps)
  with CDKSyntax {
  val stack = this
  val source = Repository.Builder.create(stack, "Source").repositoryName(getStackName).build()
  val buildEnv = BuildEnvironment
    .builder()
    .buildImage(LinuxBuildImage.STANDARD_2_0)
    .computeType(ComputeType.MEDIUM)
    .build()
  val build = PipelineProject.Builder
    .create(stack, "Build")
    .projectName(getStackName)
    .environment(buildEnv)
    .buildSpec(BuildSpec.fromSourceFilename("lambda/scala/buildspec-function.yml"))
    .build()
  val stackBuild = PipelineProject.Builder
    .create(stack, "StackBuild")
    .projectName(s"$getStackName-stack")
    .environment(buildEnv)
    .buildSpec(BuildSpec.fromSourceFilename("lambda/scala/buildspec-stack.yml"))
    .build()
  val sourceOut = new Artifact()
  val buildOut = new Artifact()
  val stackBuildOut = new Artifact()
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
            .build(),
          CodeBuildAction.Builder
            .create()
            .actionName("StackBuildAction")
            .project(stackBuild)
            .input(sourceOut)
            .outputs(list(stackBuildOut))
            .build()
        ),
        stage("Stage")(
          CloudFormationCreateReplaceChangeSetAction.Builder
            .create()
            .actionName("StageAction")
            .changeSetName(changeSetName)
            //            .templatePath(stackBuildOut.atPath(s"${conf.stackId}.template.json"))
            .templatePath(stackBuildOut.atPath(s"LambdaStack.template.json"))
            .stackName(lambdaStackName)
            .adminPermissions(true)
            .extraInputs(list(buildOut))
            .parameterOverrides(conf.code.assign(buildOut.getS3Location))
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
