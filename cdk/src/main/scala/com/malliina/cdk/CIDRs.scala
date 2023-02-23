package com.malliina.cdk

case class CIDRs(
  vpc: String,
  public1: String,
  public2: String,
  private1: String,
  private2: String
)

object CIDRs:
  val default = CIDRs(
    "10.50.0.0/16",
    "10.50.0.0/24",
    "10.50.1.0/24",
    "10.50.64.0/19",
    "10.50.96.0/19"
  )
  val default2 = CIDRs(
    "172.16.0.0/16",
    "172.16.64.0/24",
    "172.16.128.0/24",
    "172.16.64.0/19",
    "172.16.96.0/19"
  )
