package com.malliina.cdk

import software.amazon.awscdk.core.{Construct, Stack}
import software.amazon.awscdk.services.amplify
import software.amazon.awscdk.services.amplify.{AutoBranchCreation, CodeCommitSourceCodeProvider, DomainOptions}
import software.amazon.awscdk.services.codecommit.Repository

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

  val app = amplify.App.Builder
    .create(stack, "AmplifyApp")
    .appName(stackName)
    .description(s"Amplify app of $stackName")
    .sourceCodeProvider(
      CodeCommitSourceCodeProvider.Builder.create().repository(codeCommit).build()
    )
    .autoBranchCreation(
      AutoBranchCreation.builder().autoBuild(true).pullRequestPreview(true).build()
    )
    .autoBranchDeletion(true)
    .build()
  val master = app.addBranch("master")
  val dev = app.addBranch("dev")
  val domain =
    app.addDomain("Domain", DomainOptions.builder().domainName("amplify.malliina.site").build())
  domain.mapSubDomain(dev)
  outputs(stack)(
    "CodeCommitHttpsUrl" -> codeCommit.getRepositoryCloneUrlHttp,
    "AmplifyDefaultDomain" -> app.getDefaultDomain
  )
}
