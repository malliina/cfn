package com.malliina.app

import cats.effect.IO
import cats.effect.kernel.Async
import org.http4s.circe.CirceInstances
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.`Content-Type`
import org.http4s.{Charset, EntityEncoder, MediaType, syntax}
import scalatags.generic.Frag

trait MyScalatagsInstances:
  implicit def scalatagsEncoder[F[_], C <: Frag[?, String]](implicit
    charset: Charset = Charset.`UTF-8`
  ): EntityEncoder[F, C] =
    contentEncoder(MediaType.text.html)

  private def contentEncoder[F[_], C <: Frag[?, String]](
    mediaType: MediaType
  )(implicit charset: Charset): EntityEncoder[F, C] =
    EntityEncoder
      .stringEncoder[F]
      .contramap[C](content => content.render)
      .withContentType(`Content-Type`(mediaType, charset))

trait Implicits[F[_]]
  extends syntax.AllSyntax
  with Http4sDsl[F]
  with MyScalatagsInstances
  with CirceInstances
