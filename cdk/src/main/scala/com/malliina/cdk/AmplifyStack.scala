package com.malliina.cdk

import software.amazon.awscdk.{SecretValue, Stack}
import software.amazon.awscdk.services.amplify.CfnDomain.SubDomainSettingProperty
import software.amazon.awscdk.services.amplify.alpha.*
import software.amazon.awscdk.services.amplify.{CfnBranch, CfnDomain}
import software.amazon.awscdk.services.route53.{HostedZone, HostedZoneProviderProps}
import software.constructs.Construct

case class AmplifyConf(domainName: Option[String])

class AmplifyStack(conf: AmplifyConf, scope: Construct, stackName: String)
  extends Stack(scope, stackName, CDK.stackProps)
  with CDKSyntax:
  override val construct: Construct = this
  val stack = this

  val app = App.Builder
    .create(stack, "AmplifyApp")
    .make: b =>
      b.appName(stackName)
        .description(s"Amplify app of $stackName")
        .sourceCodeProvider(
          GitHubSourceCodeProvider.Builder
            .create()
            .make: b =>
              b.owner("malliina")
                .repository("cfn")
                .oauthToken(SecretValue.secretsManager("github-token"))
        )
        .autoBranchCreation(
          AutoBranchCreation
            .builder()
            .make: b =>
              b.autoBuild(true)
                .pullRequestPreview(true)
                .patterns(list("*", "**/*"))
            //        .basicAuth(BasicAuth.fromGeneratedPassword("amplify"))
        )
        .autoBranchDeletion(true)
        .environmentVariables(map("A" -> "B"))
  val master = CfnBranch.Builder
    .create(stack, "MasterBranch")
    .make: b =>
      b.appId(app.getAppId)
        .branchName("master")
        .stage("PRODUCTION")
  val amplifyDomainUrl =
    s"https://${master.getBranchName}.${app.getDefaultDomain}"
  conf.domainName.foreach: domainName =>
    val dns = HostedZone.fromLookup(
      stack,
      "Zone",
      HostedZoneProviderProps.builder().make(_.domainName(domainName))
    )
    val zoneName = dns.getZoneName
    // without this, auto subdomain doesn't work
    val domainRole = role("DomainRole"): b =>
      b.description(
        "The service role that will be used by AWS Amplify for the auto sub-domain feature."
      ).path("/service-role/")
        .assumedBy(principals.amplify)
        .inlinePolicies(
          map(
            "DomainPolicy" -> policyDocument: b =>
              b.statements(
                list(
                  allowStatement(
                    "route53:ChangeResourceRecordSets",
                    dns.getHostedZoneArn
                  ),
                  allowStatement("route53:ListHostedZones", "*")
                )
              )
          )
        )

    val domain = CfnDomain.Builder
      .create(stack, "Domain")
      .make: b =>
        b.appId(app.getAppId)
          .domainName(domainName)
          .autoSubDomainIamRole(domainRole.getRoleArn)
          .subDomainSettings(
            list(
              SubDomainSettingProperty
                .builder()
                .make: b =>
                  b.branchName(master.getBranchName)
                    .prefix("www")
            )
          )
          .enableAutoSubDomain(true)
          .autoSubDomainCreationPatterns(list("feature/*"))
    domain.addDependency(master)
    val webRoot = s"https://www.$domainName"
    app.addCustomRule(
      CustomRule.Builder
        .create()
        .make: b =>
          b.source(s"https://$domainName")
            .target(webRoot)
            .status(RedirectStatus.TEMPORARY_REDIRECT)
    )
    outputs(stack)(
      "WebRoot" -> webRoot,
      "DomainName" -> domainName
    )
  outputs(stack)(
    "AmplifyDefaultDomain" -> app.getDefaultDomain,
    "AmplifyDomainUrl" -> amplifyDomainUrl
  )
