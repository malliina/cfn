package com.malliina.cdk

import software.amazon.awscdk.services.codebuild.{BuildEnvironmentVariable, BuildEnvironmentVariableType}
import software.amazon.awscdk.services.codepipeline.{IAction, StageProps}
import software.amazon.awscdk.services.elasticbeanstalk.CfnConfigurationTemplate.ConfigurationOptionSettingProperty
import software.amazon.awscdk.services.iam.{Effect, PolicyStatement, ServicePrincipal}
import software.amazon.awscdk.{CfnOutput, CfnTag, Stack}

import java.util
import scala.jdk.CollectionConverters.{MapHasAsJava, SeqHasAsJava}

trait CDKSyntax:
  def principal(service: String) =
    ServicePrincipal.Builder.create(service).build()
  object principals:
    val amplify = principal("amplify.amazonaws.com")
  protected def allowStatement(
    action: String,
    resource: String,
    moreResources: String*
  ): PolicyStatement =
    PolicyStatement.Builder
      .create()
      .actions(list(action))
      .effect(Effect.ALLOW)
      .resources(list(resource +: moreResources*))
      .build()
  def list[T](xs: T*) = xs.asJava
  def map[T](kvs: (String, T)*): util.Map[String, T] = Map(kvs*).asJava
  def optionSetting(namespace: String, optionName: String, value: String) =
    ConfigurationOptionSettingProperty
      .builder()
      .namespace(namespace)
      .optionName(optionName)
      .value(value)
      .build()
  def tagList(kvs: (String, String)*): util.List[CfnTag] =
    kvs.map { case (k, v) => CfnTag.builder().key(k).value(v).build() }.asJava
  def outputs(scope: Stack, exportStackName: Boolean = true)(
    kvs: (String, String)*
  ) =
    kvs.map { case (k, v) =>
      CfnOutput.Builder
        .create(scope, k)
        .exportName(if exportStackName then s"${scope.getStackName}-$k" else k)
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
      .actions(list(actions*))
      .build()
