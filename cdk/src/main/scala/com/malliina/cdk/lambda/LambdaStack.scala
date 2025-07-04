package com.malliina.cdk.lambda

import com.malliina.cdk.{CDK, CDKSimpleSyntax, CDKSyntax}
import software.amazon.awscdk.pipelines.{CodePipelineSource, ShellStep}
import software.amazon.awscdk.services.lambda.{Code, Function as LambdaFunction, Runtime as LambdaRuntime}
import software.amazon.awscdk.{Stack, Stage, Duration as AWSDuration}
import software.constructs.Construct

class LambdaStack(val construct: Construct, stackName: String)
  extends Stack(construct, stackName, CDK.stackProps)
  with CDKSyntax:
  val stack = this
  private val pipeline = codePipeline(stack, "Pipeline"): p =>
    p.pipelineName(stackName)
      .synth(
        ShellStep.Builder
          .create("Synth")
          .make: b =>
            b.input(CodePipelineSource.gitHub("malliina/cfn", "master"))
              .commands(list("./cdk/build.sh"))
      )
  pipeline.addStage(LambdaStage(stack, "FunctionStage"))

class LambdaStage(scope: Construct, id: String) extends Stage(scope, id):
  val func = SimpleLambda(this, "FunctionStack")

class SimpleLambda(scope: Construct, id: String, jarTarget: String = "simple/jartarget")
  extends Stack(scope, id)
  with CDKSimpleSyntax:
  val stack = this
  val function =
    LambdaFunction.Builder
      .create(stack, "Function")
      .make: b =>
        b.runtime(LambdaRuntime.JAVA_21)
          .code(Code.fromAsset(jarTarget))
          .memorySize(512)
          .timeout(AWSDuration.seconds(180))
          .handler("com.malliina.simple.Handler::run")
