package com.smartcodeltd.metrics

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import org.scalatest._

@RunWith(classOf[JUnitRunner])
class MetricRegistrySpec extends FlatSpec with Matchers {

  "An empty Set" should "have size 0" in {
    assert(Set.empty.size == 0)
  }
}