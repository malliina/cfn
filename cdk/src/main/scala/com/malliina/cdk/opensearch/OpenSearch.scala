package com.malliina.cdk.opensearch

import com.malliina.cdk.{CDK, CDKSyntax}
import software.amazon.awscdk.Stack
import software.amazon.awscdk.services.cognito.*
import software.amazon.awscdk.services.iam.{AnyPrincipal, FederatedPrincipal, ManagedPolicy, Role}
import software.amazon.awscdk.services.lambda.IFunction
import software.amazon.awscdk.services.logs.destinations.LambdaDestination
import software.amazon.awscdk.services.logs.{FilterPattern, ILogGroup, LogGroup, SubscriptionFilter}
import software.amazon.awscdk.services.opensearchservice.*
import software.constructs.Construct

object OpenSearch:
  def stack(scope: Construct, stackName: String): OpenSearch =
    val stack = new Stack(scope, stackName, CDK.stackProps)
    new OpenSearch(stack)

class OpenSearch(stack: Stack) extends CDKSyntax:
//  val user = User.Builder.create(stack, "User").userName("opensearch").build()
//  user.addToPolicy()
  override val construct: Construct = stack

  /** This role gives Amazon OpenSearch Service permissions to configure the Amazon Cognito user and
    * identity pools and use them for OpenSearch Dashboards/Kibana authentication. It is separate
    * from the IAM role that allows users access to OpenSearch Dashboards/Kibana after they log in.
    */
  val cognitoRole = role("Role") { b =>
    b.managedPolicies(
      list(ManagedPolicy.fromAwsManagedPolicyName("AmazonOpenSearchServiceCognitoAccess"))
    ).assumedBy(principal("es.amazonaws.com"))
  }
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
  val authRole = userRole("AuthUserRole", authenticated = true)
  val unauthRole = userRole("UnauthUserRole", authenticated = false)
  def userRole(id: String, authenticated: Boolean) =
    val auth = if authenticated then "authenticated" else "unauthenticated"
    Role.Builder
      .create(stack, id)
      .assumedBy(
        new FederatedPrincipal(
          "cognito-identity.amazonaws.com",
          map(
            "StringEquals" -> map("cognito-identity.amazonaws.com:aud" -> identityPool.getRef),
            "ForAnyValue:StringLike" -> map("cognito-identity.amazonaws.com:amr" -> auth)
          ),
          "sts:AssumeRoleWithWebIdentity"
        )
      )
      .build()
  // The above role should be attached to the authenticated role of the identity pool
  val attachment = CfnIdentityPoolRoleAttachment.Builder
    .create(stack, "RoleAttachment")
    .identityPoolId(identityPool.getRef)
    .roles(map("authenticated" -> authRole.getRoleArn, "unauthenticated" -> unauthRole.getRoleArn))
    .build()
//  val pattern = FilterPattern.spaceDelimited("timestamp", "level", "logger", "", "message")
  val domain = Domain.Builder
    .create(stack, "Domain")
    .version(EngineVersion.OPENSEARCH_2_3)
    .domainName("search")
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
        .masterUserArn(authRole.getRoleArn)
        .build()
    )
    .build()
  val lambdaStreamRole = role("StreamingRole") { b =>
    b.assumedBy(principals.lambda).managedPolicies(list(policies.basicLambda))
  }
  domain.grantReadWrite(lambdaStreamRole)
  domain.grantReadWrite(authRole)
  outputs(stack)(
    "OpensearchUserPoolArn" -> userPool.getUserPoolArn,
    "OpensearchUserPoolId" -> userPool.getUserPoolId,
    "OpensearchDomainEndpoint" -> domain.getDomainEndpoint,
    "OpensearchDomainArn" -> domain.getDomainArn,
    "OpensearchIdentityPoolId" -> identityPool.getRef
  )
