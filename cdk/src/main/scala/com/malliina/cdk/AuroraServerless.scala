package com.malliina.cdk

import software.amazon.awscdk.services.cloudwatch.{ComparisonOperator, TreatMissingData, Unit as CloudWatchUnit}
import software.amazon.awscdk.services.ec2.{IVpc, Peer, Port}
import software.amazon.awscdk.services.rds.CfnDBCluster
import software.amazon.awscdk.services.rds.CfnDBCluster.ServerlessV2ScalingConfigurationProperty
import software.amazon.awscdk.services.secretsmanager.{Secret, SecretStringGenerator}
import software.amazon.awscdk.{Duration, RemovalPolicy, Stack}
import software.constructs.Construct

import scala.jdk.CollectionConverters.{ListHasAsScala, SeqHasAsJava}

object AuroraServerless:
  def stack(
    env: Env,
    appName: String,
    vpc: IVpc,
    ingressSecurityGroupIds: Seq[String],
    scope: Construct
  ): AuroraServerless =
    val stack = new Stack(
      scope,
      s"$env-$appName-database",
      CDK.stackProps
    )
    AuroraServerless(env, appName, vpc, ingressSecurityGroupIds, stack)

class AuroraServerless(
  env: Env,
  appName: String,
  vpc: IVpc,
  ingressSecurityGroupIds: Seq[String],
  stack: Stack
) extends CDKSyntax:
  override val construct: Construct = stack
  val envName = s"$env-$appName"
  val secret = Secret.Builder
    .create(stack, "Credentials")
    .make: b =>
      b.secretName(s"$env/$appName")
        .removalPolicy(RemovalPolicy.DESTROY)
        .generateSecretString(
          SecretStringGenerator
            .builder()
            .make: b =>
              b.secretStringTemplate(s"""{"username": "$appName"}""")
                .generateStringKey("password")
                .excludePunctuation(true)
                .excludeCharacters("""/@\"'\\""")
        )
  val subnet = dbSubnetGroup("Subnets"): b =>
    b.dbSubnetGroupDescription("Database subnet group")
      .subnetIds(vpc.getPrivateSubnets.asScala.map(_.getSubnetId).asJava)
  val securityGroup = secGroup("SecurityGroup", vpc): b =>
    b.description(s"Access to database $envName.")
      .allowAllOutbound(false)
  securityGroup.addEgressRule(
    Peer.anyIpv4(),
    Port.tcp(3306),
    "Allow outbound on 3306"
  )
  val appSecurityGroup = secGroup("AppSecurityGroup", vpc): b =>
    b.description("Security group for app using this database.")
  (appSecurityGroup.getSecurityGroupId +: ingressSecurityGroupIds).foreach: secGroupId =>
    securityGroup.addIngressRule(
      Peer.securityGroupId(secGroupId),
      Port.tcp(3306)
    )
  val cluster = CfnDBCluster.Builder
    .create(stack, "Database")
    .make: b =>
      b.databaseName(appName)
        .engine("aurora-mysql")
        .engineVersion("8.0.mysql_aurora.3.02.2")
        .masterUsername(resolveJson(secret.getSecretName, "username"))
        .masterUserPassword(resolveJson(secret.getSecretName, "password"))
        .serverlessV2ScalingConfiguration(
          ServerlessV2ScalingConfigurationProperty
            .builder()
            .make: b =>
              b.minCapacity(0.5)
                .maxCapacity(4)
        )
        .dbSubnetGroupName(subnet.getRef)
        .vpcSecurityGroupIds(list(securityGroup.getSecurityGroupId))
  //    .deletionProtection(true)
  val serverlessInstance = dbInstance("Instance"): b =>
    b.dbInstanceClass("db.serverless")
      .engine("aurora-mysql")
      .dbClusterIdentifier(cluster.getRef)
  val alarmTopic = topic("AlarmTopic"): b =>
    b.displayName(s"Database alarms for $envName")
  val cpuAlarmThresholdPercent = 80
  val cpuMetric = metric: b =>
    b.metricName("CPUUtilization")
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
  val cpuAlarm = alarm("CpuAlarm"): b =>
    b.alarmDescription(
      s"CPU utilization of cluster ${cluster.getRef} over $cpuAlarmThresholdPercent%"
    ).treatMissingData(TreatMissingData.NOT_BREACHING)
      .metric(cpuMetric)
      .evaluationPeriods(2)
      .threshold(cpuAlarmThresholdPercent)
      .comparisonOperator(ComparisonOperator.GREATER_THAN_OR_EQUAL_TO_THRESHOLD)
  cpuAlarm.getNode.addDependency(cluster)
  outputs(stack)(
    "AppSecurityGroupId" -> appSecurityGroup.getSecurityGroupId,
//    "DatabaseIdentifier" -> cluster.getDbClusterIdentifier,
    "DatabaseUrl" -> cluster.getAttrEndpointAddress,
    "Topic" -> alarmTopic.getTopicArn
  )
