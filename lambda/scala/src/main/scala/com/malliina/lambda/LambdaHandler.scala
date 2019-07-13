package com.malliina.lambda

import java.time.Instant

import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}

class LambdaHandler extends RequestHandler[ScheduledEvent, String] {
  override def handleRequest(event: ScheduledEvent, context: Context): String = {
    val now = Instant.now()
    println(s"Hello, Lambda! The time is now $now.")
    s"Handled event '${event.getId}'."
  }
}
