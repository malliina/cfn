package com.malliina.cdk

import com.malliina.cdk.S3WebsiteStack.WebsiteConf
import software.amazon.awscdk.core.{Construct, RemovalPolicy, Stack}
import software.amazon.awscdk.services.cloudfront.CfnDistribution
import software.amazon.awscdk.services.cloudfront.CfnDistribution._
import software.amazon.awscdk.services.iam.{AnyPrincipal, PolicyStatement}
import software.amazon.awscdk.services.route53.CfnRecordSet
import software.amazon.awscdk.services.route53.CfnRecordSet.AliasTargetProperty
import software.amazon.awscdk.services.route53.targets.CloudFrontTarget
import software.amazon.awscdk.services.s3.Bucket
import software.amazon.awscdk.services.ssm.StringParameter

object S3WebsiteStack {
  case class WebsiteConf(domain: String, hostedZoneParamName: String, certificateParamName: String)

  def apply(conf: WebsiteConf, scope: Construct, name: String): S3WebsiteStack =
    new S3WebsiteStack(conf, scope, name)
}

class S3WebsiteStack(conf: WebsiteConf, scope: Construct, stackName: String)
  extends Stack(scope, stackName, CDK.stackProps)
  with CDKSyntax {
  val stack = this
  val indexDocument = "index.html"

  val headerName = "Referer"
  val secretHeader = "secret"

  val bucket = Bucket.Builder
    .create(stack, "bucket")
    .websiteIndexDocument(indexDocument)
    .websiteErrorDocument("error.html")
    .removalPolicy(RemovalPolicy.RETAIN)
    .build()
  bucket.addToResourcePolicy(
    PolicyStatement.Builder
      .create()
      .principals(list(new AnyPrincipal()))
      .actions(list("s3:GetObject"))
      .resources(list(s"${bucket.getBucketArn}/*"))
      .conditions(map("StringEquals" -> map(s"aws:$headerName" -> list(secretHeader))))
      .build()
  )
  val viewerProtocolPolicy = "redirect-to-https"
  val bucketOrigin = "bucket"
  val cloudFront = CfnDistribution.Builder
    .create(stack, "cloudfront")
    .distributionConfig(
      DistributionConfigProperty
        .builder()
        .comment(s"Website hosting for ${conf.domain}")
        .enabled(true)
        .defaultRootObject(indexDocument)
        .aliases(list(conf.domain))
        .cacheBehaviors(
          list(
            CacheBehaviorProperty
              .builder()
              .allowedMethods(list("HEAD", "GET", "POST", "PUT", "PATCH", "OPTIONS", "DELETE"))
              .pathPattern("assets/*")
              .targetOriginId(bucketOrigin)
              .forwardedValues(
                ForwardedValuesProperty
                  .builder()
                  .queryString(true)
                  .cookies(CookiesProperty.builder().forward("none").build())
                  .build()
              )
              .viewerProtocolPolicy(viewerProtocolPolicy)
              .build()
          )
        )
        .defaultCacheBehavior(
          DefaultCacheBehaviorProperty
            .builder()
            .allowedMethods(list("HEAD", "GET"))
            .targetOriginId(bucketOrigin)
            .forwardedValues(
              ForwardedValuesProperty
                .builder()
                .queryString(true)
                .headers(list("Authorization"))
                .cookies(CookiesProperty.builder().forward("all").build())
                .build()
            )
            .viewerProtocolPolicy(viewerProtocolPolicy)
            .build()
        )
        .origins(
          list(
            OriginProperty
              .builder()
              .domainName(bucket.getBucketWebsiteDomainName)
              .id(bucketOrigin)
              .customOriginConfig(
                CustomOriginConfigProperty
                  .builder()
                  .originProtocolPolicy("http-only")
                  .build()
              )
              .originCustomHeaders(
                list(
                  OriginCustomHeaderProperty
                    .builder()
                    .headerName(headerName)
                    .headerValue(secretHeader)
                    .build()
                )
              )
              .build()
          )
        )
        .viewerCertificate(
          ViewerCertificateProperty
            .builder()
            .acmCertificateArn(StringParameter.valueFromLookup(stack, conf.certificateParamName))
            .sslSupportMethod("sni-only")
            .build()
        )
        .build()
    )
    .build()
  val dns = CfnRecordSet.Builder
    .create(stack, "dns")
    .name(conf.domain)
    .hostedZoneId(StringParameter.valueFromLookup(stack, conf.hostedZoneParamName))
    .`type`("A")
    .aliasTarget(
      AliasTargetProperty
        .builder()
        .dnsName(cloudFront.getAttrDomainName)
        .hostedZoneId(CloudFrontTarget.CLOUDFRONT_ZONE_ID)
        .build()
    )
    .build()

  val outs = outputs(stack)(
    "WebsiteURL" -> bucket.getBucketWebsiteUrl,
    "CloudFrontDomainName" -> cloudFront.getAttrDomainName,
    "DomainName" -> dns.getRef
  )
}
