package com.malliina.cdk

import com.malliina.cdk.S3Redirect.RedirectConf
import com.malliina.cdk.S3WebsiteStack.WebsiteConf
import com.malliina.cdk.StaticWebsite.StaticConf
import com.malliina.cdk.lambda.LambdaStack
import com.malliina.cdk.opensearch.OpenSearch
import software.amazon.awscdk.services.ec2.{Vpc, VpcLookupOptions}
import software.amazon.awscdk.{Environment, StackProps, App as AWSApp}

object CDK extends CDKSimpleSyntax:
  val stackProps: StackProps =
    StackProps
      .builder()
      .make: b =>
        b.env(
          Environment
            .builder()
            .make: b =>
              b.account("490166768057")
                .region("eu-north-1")
        )

  def main(args: Array[String]): Unit =
    val app = new AWSApp()

    val vpc = VPCStack(app, "cdkvpc")

    val websiteConf =
      WebsiteConf(
        "cdk.malliina.com",
        "/global/route53/zone",
        "/global/certificates/arn"
      )
    val static =
      StaticWebsite(StaticConf("cdk.malliina.com", "/global/certificates/arn"), app, "s3-static")
    val website = S3WebsiteStack(websiteConf, app, "s3-website")
    val redirect = S3Redirect(
      RedirectConf(
        "old.malliina.site",
        websiteConf.domain,
        websiteConf.hostedZoneParamName,
        websiteConf.certificateParamName
      ),
      app,
      "cdk-redirect"
    )
    val amplifyApp =
      AmplifyStack(AmplifyConf(domainName = None), app, "amplify")
    val search = OpenSearch.stack(app, "opensearch")
    val database = AuroraServerless.stack(
      Env.Qa,
      "ref",
      vpc.vpc,
      vpc.bastionSecurityGroups.map(_.getSecurityGroupId),
      app
    )
    val simple = LambdaStack(app, "simple-lambda")

    val assembly = app.synth()
