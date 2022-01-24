package com.malliina.cdk.opensearch

import com.malliina.cdk.{CDK, CDKSyntax}
import software.amazon.awscdk.Stack
import software.amazon.awscdk.services.cognito._
import software.amazon.awscdk.services.iam.{AnyPrincipal, FederatedPrincipal, ManagedPolicy, Role}
import software.amazon.awscdk.services.opensearchservice._
import software.constructs.Construct

object Opensearch {
  def stack(scope: Construct, stackName: String): Opensearch = {
    val stack = new Stack(scope, stackName, CDK.stackProps)
    new Opensearch(stack)
  }
}

class Opensearch(stack: Stack) extends CDKSyntax {

//  val user = User.Builder.create(stack, "User").userName("opensearch").build()
//  user.addToPolicy()

  /** This role gives Amazon OpenSearch Service permissions to configure the Amazon Cognito user and
    * identity pools and use them for OpenSearch Dashboards/Kibana authentication. It is separate
    * from the IAM role that allows users access to OpenSearch Dashboards/Kibana after they log in.
    */
  val cognitoRole = Role.Builder
    .create(stack, "Role")
    .managedPolicies(
      list(ManagedPolicy.fromAwsManagedPolicyName("AmazonOpenSearchServiceCognitoAccess"))
    )
    .assumedBy(principal("es.amazonaws.com"))
    .build()
  val userPool = UserPool.Builder.create(stack, "UserPool").userPoolName("opensearch").build()
  userPool.addDomain(
    "Domain",
    UserPoolDomainOptions
      .builder()
      .cognitoDomain(CognitoDomainOptions.builder().domainPrefix("malliinasearch").build())
      .build()
  )
  val identityPool =
    CfnIdentityPool.Builder
      .create(stack, "IdentityPool")
      .identityPoolName("opensearch")
      .allowUnauthenticatedIdentities(false)
      .build()
  val userRole = Role.Builder
    .create(stack, "UserRole")
    .assumedBy(
      new FederatedPrincipal(
        "cognito-identity.amazonaws.com",
        map(
          "StringEquals" -> map("cognito-identity.amazonaws.com:aud" -> identityPool.getRef),
          "ForAnyValue:StringLike" -> map("cognito-identity.amazonaws.com:amr" -> "authenticated")
        ),
        "sts:AssumeRoleWithWebIdentity"
      )
    )
    .build()
  // The above role should be attached to the authenticated role of the identity pool
  val attachment = CfnIdentityPoolRoleAttachment.Builder
    .create(stack, "RoleAttachment")
    .identityPoolId(identityPool.getRef)
    .roles(map("authenticated" -> userRole.getRoleArn))
    .build()
//  val pattern = FilterPattern.spaceDelimited("timestamp", "level", "logger", "", "message")
  val domain = Domain.Builder
    .create(stack, "Domain")
    .version(EngineVersion.OPENSEARCH_1_0)
    .domainName("opensearch")
    .enableVersionUpgrade(true)
    .cognitoDashboardsAuth(
      CognitoOptions
        .builder()
        .userPoolId(userPool.getUserPoolId)
        .identityPoolId(identityPool.getRef)
        .role(cognitoRole)
        .build()
    )
    .capacity(
      CapacityConfig
        .builder()
        .dataNodes(1)
        .dataNodeInstanceType("t3.small.search")
        .masterNodes(0)
        .build()
    )
    .nodeToNodeEncryption(true)
    .encryptionAtRest(EncryptionAtRestOptions.builder().enabled(true).build())
    .enforceHttps(true)
    .fineGrainedAccessControl(
      AdvancedSecurityOptions
        .builder()
        .masterUserArn(userRole.getRoleArn)
        .build()
    )
    .build()
  domain.grantReadWrite(new AnyPrincipal())
  outputs(stack)(
    "OpensearchUserPoolArn" -> userPool.getUserPoolArn,
    "OpensearchUserPoolId" -> userPool.getUserPoolId,
    "OpensearchDomainEndpoint" -> domain.getDomainEndpoint,
    "OpensearchDomainArn" -> domain.getDomainArn,
    "OpensearchIdentityPoolId" -> identityPool.getRef
  )
}
