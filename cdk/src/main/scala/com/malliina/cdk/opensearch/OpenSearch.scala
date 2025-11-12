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
//  val user = User.Builder.create(stack, "User").make(_.userName("opensearch"))
//  user.addToPolicy()
  override val construct: Construct = stack

  /** This role gives Amazon OpenSearch Service permissions to configure the Amazon Cognito user and
    * identity pools and use them for OpenSearch Dashboards/Kibana authentication. It is separate
    * from the IAM role that allows users access to OpenSearch Dashboards/Kibana after they log in.
    */
  val cognitoRole = role("Role"): b =>
    b.managedPolicies(
      list(ManagedPolicy.fromAwsManagedPolicyName("AmazonOpenSearchServiceCognitoAccess"))
    ).assumedBy(principal("es.amazonaws.com"))
  val userPool = UserPool.Builder.create(stack, "UserPool").make(_.userPoolName("opensearch"))
  userPool.addDomain(
    "Domain",
    UserPoolDomainOptions
      .builder()
      .make: b =>
        b.cognitoDomain(CognitoDomainOptions.builder().make(_.domainPrefix("malliinasearch")))
  )
  val identityPool =
    CfnIdentityPool.Builder
      .create(stack, "IdentityPool")
      .make: b =>
        b.identityPoolName("opensearch")
          .allowUnauthenticatedIdentities(false)
  val authRole = userRole("AuthUserRole", authenticated = true)
  val unauthRole = userRole("UnauthUserRole", authenticated = false)
  def userRole(id: String, authenticated: Boolean) =
    val auth = if authenticated then "authenticated" else "unauthenticated"
    Role.Builder
      .create(stack, id)
      .make: b =>
        b.assumedBy(
          new FederatedPrincipal(
            "cognito-identity.amazonaws.com",
            map(
              "StringEquals" -> map("cognito-identity.amazonaws.com:aud" -> identityPool.getRef),
              "ForAnyValue:StringLike" -> map("cognito-identity.amazonaws.com:amr" -> auth)
            ),
            "sts:AssumeRoleWithWebIdentity"
          )
        )
  // The above role should be attached to the authenticated role of the identity pool
  val attachment = CfnIdentityPoolRoleAttachment.Builder
    .create(stack, "RoleAttachment")
    .make: b =>
      b.identityPoolId(identityPool.getRef)
        .roles(
          map("authenticated" -> authRole.getRoleArn, "unauthenticated" -> unauthRole.getRoleArn)
        )
//  val pattern = FilterPattern.spaceDelimited("timestamp", "level", "logger", "", "message")
  val domain = Domain.Builder
    .create(stack, "Domain")
    .make: b =>
      b.version(EngineVersion.OPENSEARCH_2_3)
        .domainName("search")
        .enableVersionUpgrade(true)
        .cognitoDashboardsAuth(
          CognitoOptions
            .builder()
            .make: b =>
              b.userPoolId(userPool.getUserPoolId)
                .identityPoolId(identityPool.getRef)
                .role(cognitoRole)
        )
        .capacity(
          CapacityConfig
            .builder()
            .make: b =>
              b.dataNodes(1)
                .dataNodeInstanceType("t3.small.search")
                .masterNodes(0)
        )
        .nodeToNodeEncryption(true)
        .encryptionAtRest(EncryptionAtRestOptions.builder().make(_.enabled(true)))
        .enforceHttps(true)
        .fineGrainedAccessControl(
          AdvancedSecurityOptions
            .builder()
            .make: b =>
              b.masterUserArn(authRole.getRoleArn)
        )
  val lambdaStreamRole = role("StreamingRole"): b =>
    b.assumedBy(principals.lambda).managedPolicies(list(policies.basicLambda))
  domain.grantReadWrite(lambdaStreamRole)
  domain.grantReadWrite(authRole)
  outputs(stack)(
    "OpensearchUserPoolArn" -> userPool.getUserPoolArn,
    "OpensearchUserPoolId" -> userPool.getUserPoolId,
    "OpensearchDomainEndpoint" -> domain.getDomainEndpoint,
    "OpensearchDomainArn" -> domain.getDomainArn,
    "OpensearchIdentityPoolId" -> identityPool.getRef
  )
