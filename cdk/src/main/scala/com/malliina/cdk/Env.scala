package com.malliina.cdk

enum Env(val name: String):
  override def toString = name
  case Qa extends Env("qa")
