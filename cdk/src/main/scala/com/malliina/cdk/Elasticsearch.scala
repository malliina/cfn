package com.malliina.cdk

import software.amazon.awscdk.core.Stack
import software.amazon.awscdk.services.elasticsearch.{CapacityConfig, Domain, ElasticsearchVersion}

class Elasticsearch(stack: Stack) extends CDKSyntax {
  val domain = Domain.Builder
    .create(stack, "Domain")
    .version(ElasticsearchVersion.V7_10)
    .enableVersionUpgrade(true)
    .capacity(
      CapacityConfig
        .builder()
        .dataNodes(1)
        .dataNodeInstanceType("t3.small.search")
        .masterNodes(0)
        .build()
    )
    .build()

  outputs(stack)(
    "ElasticsearchDomainEndpoint" -> domain.getDomainEndpoint,
    "ElasticsearchDomainArn" -> domain.getDomainArn
  )
}
