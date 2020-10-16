package com.malliina.cdk

import software.amazon.awscdk.core.{CfnOutput, Stack}
import software.amazon.awscdk.services.codebuild.{BuildEnvironmentVariable, BuildEnvironmentVariableType}
import software.amazon.awscdk.services.codepipeline.{IAction, StageProps}
import software.amazon.awscdk.services.elasticbeanstalk.CfnConfigurationTemplate.ConfigurationOptionSettingProperty
import software.amazon.awscdk.services.iam.ServicePrincipal

import scala.jdk.CollectionConverters.{MapHasAsJava, SeqHasAsJava}

trait CDKSyntax {
  def principal(service: String) = ServicePrincipal.Builder.create(service).build()
  def list[T](xs: T*) = xs.asJava
  def map[T](kvs: (String, T)*) = Map(kvs: _*).asJava
  def optionSetting(namespace: String, optionName: String, value: String) =
    ConfigurationOptionSettingProperty
      .builder()
      .namespace(namespace)
      .optionName(optionName)
      .value(value)
      .build()
  def outputs(scope: Stack)(kvs: (String, String)*) = kvs.map {
    case (k, v) =>
      CfnOutput.Builder
        .create(scope, k)
        .exportName(k)
        .value(v)
        .build()
  }

  def buildEnv(value: String) =
    BuildEnvironmentVariable
      .builder()
      .`type`(BuildEnvironmentVariableType.PLAINTEXT)
      .value(value)
      .build()

  def stage(name: String)(actions: IAction*) =
    StageProps
      .builder()
      .stageName(name)
      .actions(list(actions: _*))
      .build()
}
