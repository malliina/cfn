package com.malliina.cdk

import software.amazon.awscdk.core.{CfnOutput, Stack}
import software.amazon.awscdk.services.elasticbeanstalk.CfnConfigurationTemplate.ConfigurationOptionSettingProperty
import software.amazon.awscdk.services.iam.ServicePrincipal

import scala.jdk.CollectionConverters.{MapHasAsJava, SeqHasAsJava}

trait CDKBuilders {
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
}

trait CDKSyntax extends CDKBuilders
