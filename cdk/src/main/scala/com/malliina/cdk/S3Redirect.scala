package com.malliina.cdk

import com.malliina.cdk.S3Redirect.RedirectConf
import software.amazon.awscdk.Stack
import software.amazon.awscdk.services.cloudfront.CfnDistribution
import software.amazon.awscdk.services.cloudfront.CfnDistribution.*
import software.amazon.awscdk.services.route53.CfnRecordSet
import software.amazon.awscdk.services.route53.CfnRecordSet.AliasTargetProperty
import software.amazon.awscdk.services.route53.targets.CloudFrontTarget
import software.amazon.awscdk.services.s3.{Bucket, RedirectProtocol, RedirectTarget}
import software.amazon.awscdk.services.ssm.StringParameter
import software.constructs.Construct

object S3Redirect:
  case class RedirectConf(
    fromDomain: String,
    toDomain: String,
    hostedZoneParamName: String,
    certificateParamName: String
  )

class S3Redirect(conf: RedirectConf, scope: Construct, id: String)
  extends Stack(scope, id, CDK.stackProps)
  with CDKSyntax:
  val stack = this
  override val construct: Construct = stack

  val bucket = Bucket.Builder
    .create(stack, "redirect")
    .make: b =>
      b.websiteRedirect(
        RedirectTarget
          .builder()
          .make: b =>
            b.hostName(conf.toDomain)
              .protocol(RedirectProtocol.HTTPS)
      )
  val viewerProtocolPolicy = "redirect-to-https"
  val bucketOrigin = "bucket"
  val cloudFront = CfnDistribution.Builder
    .create(stack, "cloudfront")
    .make: b =>
      b.distributionConfig(
        DistributionConfigProperty
          .builder()
          .make: b =>
            b.comment(s"Redirect from ${conf.fromDomain} to ${conf.toDomain}")
              .enabled(true)
              .aliases(list(conf.fromDomain))
              .defaultCacheBehavior(
                DefaultCacheBehaviorProperty
                  .builder()
                  .make: b =>
                    b.allowedMethods(list("HEAD", "GET"))
                      .targetOriginId(bucketOrigin)
                      .forwardedValues(
                        ForwardedValuesProperty
                          .builder()
                          .make: b =>
                            b.queryString(true)
                              .cookies(CookiesProperty.builder().make(_.forward("none")))
                      )
                      .viewerProtocolPolicy(viewerProtocolPolicy)
              )
              .origins(
                list(
                  OriginProperty
                    .builder()
                    .make: b =>
                      b.domainName(bucket.getBucketWebsiteDomainName)
                        .id(bucketOrigin)
                        .customOriginConfig(
                          CustomOriginConfigProperty
                            .builder()
                            .make: b =>
                              b.originProtocolPolicy("http-only")
                        )
                )
              )
              .viewerCertificate(
                ViewerCertificateProperty
                  .builder()
                  .make: b =>
                    b.acmCertificateArn(stringParameter(conf.certificateParamName))
                      .sslSupportMethod("sni-only")
              )
      )
  val hostedZoneId = stringParameter(conf.hostedZoneParamName)
  val dns = CfnRecordSet.Builder
    .create(stack, "dns")
    .make: b =>
      b.`type`("A")
        .name(conf.fromDomain)
        .hostedZoneId(hostedZoneId)
        .aliasTarget(
          AliasTargetProperty
            .builder()
            .make: b =>
              b.dnsName(cloudFront.getAttrDomainName)
                .hostedZoneId(CloudFrontTarget.CLOUDFRONT_ZONE_ID)
        )
