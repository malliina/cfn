package com.malliina.website.Build

import java.io.Closeable
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}

import scalatags.Text.all._

object Build {
  def main(args: Array[String]): Unit = {
    val targetDir =
      sys.env.get("WEBSITE_TARGET").map(s => Paths.get(s)).getOrElse(Paths.get("target/website"))
    buildWebsite(targetDir)
  }

  def buildWebsite(targetDir: Path): Path = {
    if (!Files.exists(targetDir))
      Files.createDirectories(targetDir)
    val file = targetDir.resolve("index.html")
    Files.write(file, index.render.getBytes(StandardCharsets.UTF_8))
  }

  def index = html(body(p("Hello, world!")))

  def using[T <: Closeable, U](t: T)(code: T => U): U =
    try code(t)
    finally t.close()
}
