package com.malliina.cdk

import com.malliina.cdk.S3Redirect.RedirectConf
import software.amazon.awscdk.core.Stack
import software.amazon.awscdk.services.cloudfront.CfnDistribution
import software.amazon.awscdk.services.cloudfront.CfnDistribution.{CacheBehaviorProperty, CookiesProperty, CustomOriginConfigProperty, DefaultCacheBehaviorProperty, DistributionConfigProperty, ForwardedValuesProperty, OriginCustomHeaderProperty, OriginProperty, ViewerCertificateProperty}
import software.amazon.awscdk.services.route53.CfnRecordSet
import software.amazon.awscdk.services.route53.CfnRecordSet.AliasTargetProperty
import software.amazon.awscdk.services.route53.targets.CloudFrontTarget
import software.amazon.awscdk.services.s3.{Bucket, RedirectProtocol, RedirectTarget}
import software.amazon.awscdk.services.ssm.StringParameter
import software.constructs.Construct

object S3Redirect {
  case class RedirectConf(
    fromDomain: String,
    toDomain: String,
    hostedZoneParamName: String,
    certificateParamName: String
  )
  def apply(conf: RedirectConf, scope: Construct, id: String): S3Redirect =
    new S3Redirect(conf, scope, id)
}

class S3Redirect(conf: RedirectConf, scope: Construct, id: String)
  extends Stack(scope, id, CDK.stackProps)
  with CDKSyntax {
  val stack = this

  val bucket = Bucket.Builder
    .create(stack, "redirect")
    .websiteRedirect(
      RedirectTarget.builder().hostName(conf.toDomain).protocol(RedirectProtocol.HTTPS).build()
    )
    .build()
  val viewerProtocolPolicy = "redirect-to-https"
  val bucketOrigin = "bucket"
  val cloudFront = CfnDistribution.Builder
    .create(stack, "cloudfront")
    .distributionConfig(
      DistributionConfigProperty
        .builder()
        .comment(s"Redirect from ${conf.fromDomain} to ${conf.toDomain}")
        .enabled(true)
        .aliases(list(conf.fromDomain))
        .defaultCacheBehavior(
          DefaultCacheBehaviorProperty
            .builder()
            .allowedMethods(list("HEAD", "GET"))
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
  val hostedZoneId = StringParameter.valueFromLookup(stack, conf.hostedZoneParamName)
  val dns = CfnRecordSet.Builder
    .create(stack, "dns")
    .`type`("A")
    .name(conf.fromDomain)
    .hostedZoneId(hostedZoneId)
    .aliasTarget(
      AliasTargetProperty
        .builder()
        .dnsName(cloudFront.getAttrDomainName)
        .hostedZoneId(CloudFrontTarget.CLOUDFRONT_ZONE_ID)
        .build()
    )
    .build()
}
