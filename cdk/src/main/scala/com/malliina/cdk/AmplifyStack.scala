package com.malliina.cdk

import software.amazon.awscdk.Stack
import software.amazon.awscdk.services.amplify.alpha.{App, AutoBranchCreation, CodeCommitSourceCodeProvider, CustomRule, DomainOptions, RedirectStatus}
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
  val domainName = "malliina.site"
  val appDomain = app.addDomain(
    "Domain",
    DomainOptions
      .builder()
      .domainName(domainName)
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
    "CodeCommitHttpsUrl" -> codeCommit.getRepositoryCloneUrlHttp,
    "AmplifyDefaultDomain" -> app.getDefaultDomain,
    "WebRoot" -> webRoot
  )
}
