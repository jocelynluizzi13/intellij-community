package com.intellij.driver.sdk

import com.intellij.driver.sdk.ui.components.UiComponent
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun waitFor(
  duration: Duration = 5.seconds,
  interval: Duration = 1.seconds,
  errorMessage: String = "",
  condition: () -> Boolean
) {
  waitFor(duration = duration, interval = interval, errorMessage = errorMessage, getter = condition, checker = { it })
}

fun waitFor(
  duration: Duration = 5.seconds,
  interval: Duration = 1.seconds,
  errorMessage: () -> String,
  condition: () -> Boolean
) {
  waitFor(duration = duration, interval = interval, errorMessage = errorMessage, getter = condition, checker = { it })
}

fun <T> waitNotNull(
  duration: Duration = 5.seconds,
  interval: Duration = 1.seconds,
  errorMessage: String = "",
  getter: () -> T?
): T {
  return waitFor(duration = duration, interval = interval, errorMessage = errorMessage, getter = getter, checker = { it != null })!!
}

fun <T> waitFor(
  duration: Duration = 5.seconds,
  interval: Duration = 1.seconds,
  errorMessage: String = "",
  getter: () -> T,
  checker: (T) -> Boolean
): T {
  return waitFor(duration = duration, interval = interval, errorMessage = { errorMessage }, getter = getter, checker = checker)
}

fun <T> waitFor(
  duration: Duration = 5.seconds,
  interval: Duration = 1.seconds,
  errorMessage: () -> String = { "" },
  getter: () -> T,
  checker: (T) -> Boolean
): T {
  val endTime = System.currentTimeMillis() + duration.inWholeMilliseconds
  var now = System.currentTimeMillis()
  var result = getter()
  while (now < endTime && checker(result).not()) {
    Thread.sleep(interval.inWholeMilliseconds)
    result = getter()
    now = System.currentTimeMillis()
  }
  if (checker(result).not()) {
    throw WaitForException(duration, errorMessage() + if (result !is Boolean) " Actual: $result" else "")
  }
  else {
    return result
  }
}

class WaitForException(val duration: Duration, val errorMessage: String, cause: Throwable? = null) : IllegalStateException("Timeout($duration): $errorMessage", cause)

fun <T : UiComponent> T.wait(duration: Duration): T {
  Thread.sleep(duration.inWholeMilliseconds)
  return this
}