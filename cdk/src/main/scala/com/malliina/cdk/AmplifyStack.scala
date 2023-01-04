package com.malliina.cdk

import com.malliina.cdk.AmplifyStack.AmplifyConf
import software.amazon.awscdk.Stack
import software.amazon.awscdk.services.amplify.CfnDomain.SubDomainSettingProperty
import software.amazon.awscdk.services.amplify.alpha.*
import software.amazon.awscdk.services.amplify.{CfnBranch, CfnDomain}
import software.amazon.awscdk.services.codecommit.Repository
import software.amazon.awscdk.services.iam.{PolicyDocument, Role}
import software.amazon.awscdk.services.route53.{HostedZone, HostedZoneProviderProps}
import software.constructs.Construct

object AmplifyStack:
  case class AmplifyConf(domainName: Option[String])

class AmplifyStack(conf: AmplifyConf, scope: Construct, stackName: String)
  extends Stack(scope, stackName, CDK.stackProps)
  with CDKSyntax:
  val stack = this

  val codeCommit =
    Repository.Builder.create(stack, "Repo").repositoryName(stackName).build()

  val app = App.Builder
    .create(stack, "AmplifyApp")
    .appName(stackName)
    .description(s"Amplify app of $stackName")
    .sourceCodeProvider(
      CodeCommitSourceCodeProvider.Builder
        .create()
        .repository(codeCommit)
        .build()
    )
    .autoBranchCreation(
      AutoBranchCreation
        .builder()
        .autoBuild(true)
        .pullRequestPreview(true)
        .patterns(list("*", "**/*"))
//        .basicAuth(BasicAuth.fromGeneratedPassword("amplify"))
        .build()
    )
    .autoBranchDeletion(true)
    .environmentVariables(map("A" -> "B"))
    .build()
  val master = CfnBranch.Builder
    .create(stack, "MasterBranch")
    .appId(app.getAppId)
    .branchName("master")
    .stage("PRODUCTION")
    .build()
  val amplifyDomainUrl =
    s"https://${master.getBranchName}.${app.getDefaultDomain}"
  conf.domainName.foreach { domainName =>
    val dns = HostedZone.fromLookup(
      stack,
      "Zone",
      HostedZoneProviderProps.builder().domainName(domainName).build()
    )
    val zoneName = dns.getZoneName
    // without this, auto subdomain doesn't work
    val domainRole = Role.Builder
      .create(stack, "DomainRole")
      .description(
        "The service role that will be used by AWS Amplify for the auto sub-domain feature."
      )
      .path("/service-role/")
      .assumedBy(principals.amplify)
      .inlinePolicies(
        map(
          "DomainPolicy" -> PolicyDocument.Builder
            .create()
            .statements(
              list(
                allowStatement(
                  "route53:ChangeResourceRecordSets",
                  dns.getHostedZoneArn
                ),
                allowStatement("route53:ListHostedZones", "*")
              )
            )
            .build()
        )
      )
      .build()
    val domain = CfnDomain.Builder
      .create(stack, "Domain")
      .appId(app.getAppId)
      .domainName(domainName)
      .autoSubDomainIamRole(domainRole.getRoleArn)
      .subDomainSettings(
        list(
          SubDomainSettingProperty
            .builder()
            .branchName(master.getBranchName)
            .prefix("www")
            .build()
        )
      )
      .enableAutoSubDomain(true)
      .autoSubDomainCreationPatterns(list("feature/*"))
      .build()
    domain.addDependency(master)
    val webRoot = s"https://www.$domainName"
    app.addCustomRule(
      CustomRule.Builder
        .create()
        .source(s"https://$domainName")
        .target(webRoot)
        .status(RedirectStatus.TEMPORARY_REDIRECT)
        .build()
    )
    outputs(stack)(
      "WebRoot" -> webRoot,
      "DomainName" -> domainName
    )
  }
  outputs(stack)(
    "CodeCommitHttpsUrl" -> codeCommit.getRepositoryCloneUrlHttp,
    "AmplifyDefaultDomain" -> app.getDefaultDomain,
    "AmplifyDomainUrl" -> amplifyDomainUrl
  )
