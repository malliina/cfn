package com.malliina.cdk

import software.amazon.awscdk.Stack
import software.amazon.awscdk.services.amplify.CfnDomain.SubDomainSettingProperty
import software.amazon.awscdk.services.amplify.alpha.{App, AutoBranchCreation, BasicAuth, CodeCommitSourceCodeProvider, DomainOptions}
import software.amazon.awscdk.services.amplify.{CfnBranch, CfnDomain}
import software.amazon.awscdk.services.codecommit.Repository
import software.constructs.Construct

object AmplifyStack {
  def apply(conf: AmplifyConf, scope: Construct, stackName: String): AmplifyStack =
    new AmplifyStack(scope, stackName)

  case class AmplifyConf(int: Int)
}

class AmplifyStack(scope: Construct, stackName: String)
  extends Stack(scope, stackName, CDK.stackProps)
  with CDKSyntax {
  val stack = this

  val codeCommit = Repository.Builder.create(stack, "Repo").repositoryName(stackName).build()

  val app = App.Builder
    .create(stack, "AmplifyApp")
    .appName(stackName)
    .description(s"Amplify app of $stackName")
    .sourceCodeProvider(
      CodeCommitSourceCodeProvider.Builder.create().repository(codeCommit).build()
    )
    .autoBranchCreation(
      AutoBranchCreation
        .builder()
        .autoBuild(true)
        .pullRequestPreview(true)
        .patterns(list("*"))
//        .basicAuth(BasicAuth.fromGeneratedPassword("amplify"))
        .build()
    )
    .autoBranchDeletion(true)
    .environmentVariables(map("A" -> "B"))
    .build()
//  val master = CfnBranch.Builder
//    .create(stack, "MasterBranch")
//    .appId(app.getAppId)
//    .branchName("master")
//    .stage("PRODUCTION")
//    .build()
//  val domain = CfnDomain.Builder
//    .create(stack, "Domain")
//    .appId(app.getAppId)
//    .domainName("malliina.site")
//    .subDomainSettings(
//      list(
//        SubDomainSettingProperty
//          .builder()
//          .branchName(master.getBranchName)
//          .prefix("www")
//          .build()
//      )
//    )
//    .enableAutoSubDomain(true)
//    .autoSubDomainCreationPatterns(list("*", "pr*"))
//    .build()
//  domain.addDependsOn(master)
  val appDomain = app.addDomain(
    "Domain",
    DomainOptions
      .builder()
      .domainName("malliina.site")
      .enableAutoSubdomain(true)
      .autoSubdomainCreationPatterns(list("*", "pr*"))
      .build()
  )
  val master = app.addBranch("master")
  appDomain.mapRoot(master)
  appDomain.mapSubDomain(master, "www")
  val dev = app.addBranch("dev")
  dev.addEnvironment("STAGE", "dev")
  appDomain.mapSubDomain(dev)

  outputs(stack)(
    "CodeCommitHttpsUrl" -> codeCommit.getRepositoryCloneUrlHttp,
    "AmplifyDefaultDomain" -> app.getDefaultDomain
  )
}
