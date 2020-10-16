package com.malliina.cdk

import software.amazon.awscdk.core.{Construct, Duration, Stack}
import software.amazon.awscdk.services.lambda.{CfnParametersCode, Function => LambdaFunction, Runtime => LambdaRuntime}

object LambdaStack {
  def apply(scope: Construct, stackName: String): LambdaStack = new LambdaStack(scope, stackName)
}

class LambdaStack(scope: Construct, stackName: String)
  extends Stack(scope, stackName, CDK.stackProps) {
  val stack = this
  val code = CfnParametersCode.Builder.create().build()
  val function = LambdaFunction.Builder
    .create(stack, "Function")
    .handler("com.malliina.lambda.LambdaHandler")
    .runtime(LambdaRuntime.JAVA_11)
    .code(code)
    .memorySize(256)
    .timeout(Duration.seconds(60))
    .build()
}
