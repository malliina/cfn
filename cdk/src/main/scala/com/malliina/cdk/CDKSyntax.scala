package com.malliina.cdk

import software.amazon.awscdk.services.codebuild.{BuildEnvironmentVariable, BuildEnvironmentVariableType}
import software.amazon.awscdk.services.codepipeline.{IAction, StageProps}
import software.amazon.awscdk.services.ec2.{IVpc, SecurityGroup, Vpc}
import software.amazon.awscdk.services.elasticbeanstalk.CfnConfigurationTemplate.ConfigurationOptionSettingProperty
import software.amazon.awscdk.services.iam.{Effect, ManagedPolicy, PolicyStatement, ServicePrincipal}
import software.amazon.awscdk.services.rds.{CfnDBInstance, CfnDBSubnetGroup}
import software.amazon.awscdk.services.sns.Topic
import software.amazon.awscdk.{CfnOutput, CfnTag, Stack}

import java.util
import scala.jdk.CollectionConverters.{MapHasAsJava, SeqHasAsJava}
import software.amazon.jsii.Builder as CfnBuilder
import software.constructs.Construct

trait CDKSyntax:
  def principal(service: String) =
    ServicePrincipal.Builder.create(service).build()
  object principals:
    val amplify = principal("amplify.amazonaws.com")
    val lambda = principal("lambda.amazonaws.com")
  object policies:
    val basicLambda =
      ManagedPolicy.fromAwsManagedPolicyName("service-role/AWSLambdaBasicExecutionRole")
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
  def tagList(kvs: (String, String)*): util.List[CfnTag] =
    kvs.map { case (k, v) => CfnTag.builder().key(k).value(v).build() }.asJava
  def resolveJson(secretName: String, secretKey: String) =
    s"{{resolve:secretsmanager:$secretName::$secretKey}}"
  def outputs(scope: Stack, exportStackName: Boolean = true)(
    kvs: (String, String)*
  ) =
    kvs.map { case (k, v) =>
      val exportName =
        if exportStackName then s"${scope.getStackName}-$k" else k
      init(CfnOutput.Builder.create(scope, k)) { b =>
        b.exportName(exportName)
          .value(v)
      }
    }
  def optionSetting(namespace: String, optionName: String, value: String) =
    init(ConfigurationOptionSettingProperty.builder()) { b =>
      b.namespace(namespace).optionName(optionName).value(value)
    }
  def buildEnv(value: String) =
    init(BuildEnvironmentVariable.builder()) { b =>
      b.`type`(BuildEnvironmentVariableType.PLAINTEXT).value(value)
    }
  def stage(name: String)(actions: IAction*) =
    init(StageProps.builder()) { b =>
      b.stageName(name).actions(list(actions*))
    }
  def buildVpc(construct: Construct, id: String)(f: Vpc.Builder => Vpc.Builder) =
    init(Vpc.Builder.create(construct, id)) { b =>
      f(b)
    }
  def secGroup(construct: Construct, id: String, vpc: IVpc)(
    f: SecurityGroup.Builder => SecurityGroup.Builder
  ) =
    init(SecurityGroup.Builder.create(construct, id)) { b =>
      f(b.vpc(vpc))
    }
  def topic(construct: Construct, id: String)(f: Topic.Builder => Topic.Builder) =
    init(Topic.Builder.create(construct, id)) { b =>
      f(b)
    }
  def dbInstance(construct: Construct, id: String)(
    f: CfnDBInstance.Builder => CfnDBInstance.Builder
  ) =
    init(CfnDBInstance.Builder.create(construct, id))(f)
  def dbSubnetGroup(construct: Construct, id: String)(
    f: CfnDBSubnetGroup.Builder => CfnDBSubnetGroup.Builder
  ) =
    init(CfnDBSubnetGroup.Builder.create(construct, id)) { b =>
      f(b)
    }
  private def init[T, B <: CfnBuilder[T]](b: B)(f: B => B): T = f(b).build()
