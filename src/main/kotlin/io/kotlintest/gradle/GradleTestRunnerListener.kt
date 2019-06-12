package io.kotlintest.gradle

class GradleTestRunnerListener {

  fun engineStarted() {
    println("Engine started")
  }

  fun engineFinished(t: Throwable?) {
    println("Engine finished $t")
  }

  fun beforeSpecClass(spec: String) {
    println("Starting spec $spec")
  }

  fun afterSpecClass(spec: String, t: Throwable?) {
    println("Finished spec $spec $t")
  }

  fun beforeTestCase(testCase: String) {
    println("Starting test $testCase")
  }

  fun afterTestCase(testCase: String, result: String) {
    println("Finished test $testCase $result")
  }
}