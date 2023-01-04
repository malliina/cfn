package com.malliina.cdk

import software.amazon.awscdk.services.cloudwatch.actions.SnsAction
import software.amazon.awscdk.services.cloudwatch.{Alarm, CfnAlarm, ComparisonOperator, Metric, TreatMissingData, Unit as CloudWatchUnit}
import software.amazon.awscdk.services.ec2.{IVpc, Peer, Port, SecurityGroup}
import software.amazon.awscdk.{Duration, RemovalPolicy, SecretValue, SecretsManagerSecretOptions, Stack}
import software.amazon.awscdk.services.rds.{AuroraMysqlClusterEngineProps, AuroraMysqlEngineVersion, CfnDBCluster, CfnDBInstance, CfnDBSubnetGroup, DatabaseCluster, DatabaseClusterEngine, DatabaseSecret}
import software.amazon.awscdk.services.rds.CfnDBCluster.{ScalingConfigurationProperty, ServerlessV2ScalingConfigurationProperty}
import software.amazon.awscdk.services.sns.Topic
import software.amazon.awscdk.services.secretsmanager.{Secret, SecretStringGenerator}
import software.constructs.Construct

import scala.jdk.CollectionConverters.{ListHasAsScala, SeqHasAsJava}

object AuroraServerless:
  def stack(
    env: Env,
    appName: String,
    vpc: IVpc,
    bastionSecurityGroup: String,
    scope: Construct
  ): AuroraServerless =
    val stack = new Stack(
      scope,
      s"$env-$appName-database",
      CDK.stackProps
    )
    AuroraServerless(env, appName, vpc, bastionSecurityGroup, stack)

class AuroraServerless(
  env: Env,
  appName: String,
  vpc: IVpc,
  bastionSecurityGroup: String,
  stack: Stack
) extends CDKSyntax:
  val envName = s"$env-$appName"
  val secret = Secret.Builder
    .create(stack, "Credentials")
    .secretName(s"$env/$appName")
    .removalPolicy(RemovalPolicy.DESTROY)
    .generateSecretString(
      SecretStringGenerator
        .builder()
        .secretStringTemplate(s"""{"username": "$appName"}""")
        .generateStringKey("password")
        .excludePunctuation(true)
        .excludeCharacters("""/@\"'\\""")
        .build()
    )
    .build()
  val subnet = CfnDBSubnetGroup.Builder
    .create(stack, "Subnets")
    .dbSubnetGroupDescription("Database subnet group")
    .subnetIds(vpc.getPrivateSubnets.asScala.map(_.getSubnetId).asJava)
    .build()
  val securityGroup = SecurityGroup.Builder
    .create(stack, "SecurityGroup")
    .description(s"Access to database $envName.")
    .allowAllOutbound(false)
    .vpc(vpc)
    .build()
  securityGroup.addEgressRule(
    Peer.anyIpv4(),
    Port.tcp(3306),
    "Allow outbound on 3306"
  )
  securityGroup.addIngressRule(
    Peer.securityGroupId(bastionSecurityGroup),
    Port.tcp(3306)
  )
  val cluster = CfnDBCluster.Builder
    .create(stack, "Database")
    .databaseName(appName)
    .engine("aurora-mysql")
    .engineVersion("8.0.mysql_aurora.3.02.2")
    .masterUsername(
      s"{{resolve:secretsmanager:${secret.getSecretName}::username}}"
    )
    .masterUserPassword(
      s"{{resolve:secretsmanager:${secret.getSecretName}::password}}"
    )
    .serverlessV2ScalingConfiguration(
      ServerlessV2ScalingConfigurationProperty
        .builder()
        .minCapacity(0.5)
        .maxCapacity(4)
        .build()
    )
    .dbSubnetGroupName(subnet.getRef)
    .vpcSecurityGroupIds(list(securityGroup.getSecurityGroupId))
//    .deletionProtection(true)
    .build()
  val serverlessInstance =
    CfnDBInstance.Builder
      .create(stack, "Instance")
      .dbInstanceClass("db.serverless")
      .engine("aurora-mysql")
      .dbClusterIdentifier(cluster.getRef)
      .build()
  val alarmTopic =
    Topic.Builder
      .create(stack, "AlarmTopic")
      .displayName(s"Database alarms for $envName")
      .build()
  val cpuAlarmThresholdPercent = 80
  val cpuMetric = Metric.Builder
    .create()
    .metricName("CPUUtilization")
    .namespace("AWS/RDS")
    .unit(CloudWatchUnit.PERCENT)
    .statistic("Average")
    .period(Duration.seconds(300))
    .dimensionsMap(
      map(
        "DBClusterIdentifier" -> cluster.getRef,
        "Role" -> "WRITER"
      )
    )
    .build()
  val cpuAlarm = Alarm.Builder
    .create(stack, "CpuAlarm")
    .alarmDescription(
      s"CPU utilization of cluster ${cluster.getRef} over $cpuAlarmThresholdPercent%"
    )
    .treatMissingData(TreatMissingData.NOT_BREACHING)
    .metric(cpuMetric)
    .evaluationPeriods(2)
    .threshold(cpuAlarmThresholdPercent)
    .comparisonOperator(ComparisonOperator.GREATER_THAN_OR_EQUAL_TO_THRESHOLD)
    .build()
  cpuAlarm.getNode.addDependency(cluster)
  val outs = outputs(stack)(
//    "DatabaseIdentifier" -> cluster.getDbClusterIdentifier,
    "DatabaseUrl" -> cluster.getAttrEndpointAddress,
    "Topic" -> alarmTopic.getTopicArn
  )
