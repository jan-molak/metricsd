package com.smartcodeltd.statsd

import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.junit.JUnitRunner
import org.scalatest.prop._
import org.scalatest.{Matchers, PropSpec}

import scala.util.{Failure, Success}


@RunWith(classOf[JUnitRunner])
class ParserSpec extends PropSpec with GeneratorDrivenPropertyChecks with Matchers {
  import Gen._
  import Parser.parse

  /* note: handling values up to (2^64 - 1) requires Java8 as former implementations of Long only handle (-2^63, 2^63-1)
  *  https://github.com/b/statsd_spec
  *  http://statsd.readthedocs.org/en/latest/types.html
  *
  *  todo: compare statsd_spec with etsy's statsd helper unit tests
  *  todo: support gauge deltas as per https://github.com/etsy/statsd/commit/3eecd18
  */

  property("Value of a Gauge is within [0, 2^64^)") {
    forAll(validMetricName, positiveValue) { (name, value) =>
      parse(s"$name:$value|g") should equal(Success(GaugeSample(name, value)))
    }
  }

  property("Value of a Counter is within (-2^63^, 2^63^)") {
    forAll(validMetricName, anyValue) { (name, value) =>
      parse(s"$name:$value|c") should equal(Success(CounterSample(name, value)))
    }
  }

  property("Value of Counter's sampling rate is a positive double") {
    forAll(validMetricName, anyValue, sampleRate) { (name, value, rate) =>
      parse(s"$name:$value|c|@$rate") should equal(Success(CounterSample(name, value, rate)))
    }
  }

  property("Value of a Histogram is within [0, 2^64^)") {
    forAll(validMetricName, positiveValue) { (name, value) =>
      parse(s"$name:$value|h") should equal(Success(HistogramSample(name, value)))
    }
  }

  property("Value of a Meter is within [0, 2^64^)") {
    forAll(validMetricName, positiveValue) { (name, value) =>
      parse(s"$name:$value|m") should equal(Success(MeterSample(name, value)))
    }
  }

  property("Parsing an invalid sample results in a Failure") {
    forAll(validMetricName, invalidValue, invalidType) { (name, value, t) =>
      parse(s"$name:$value|$t").isFailure should equal(true)
    }
  }

  // -- generators

  val validMetricName  = (for { segments <- nonEmptyListOf(identifier) } yield segments.mkString(".")).suchThat(_.length > 0)
  val positiveValue = posNum[Long]
  val anyValue      = choose[Long](Long.MinValue, Long.MaxValue)
  val sampleRate    = choose[Double](0.0, 1.0).suchThat(_ > 0)

  val invalidValue       = identifier
  val invalidType        = (for {
    t1 <- alphaLowerChar
    t2 <- listOf(alphaLowerChar)
  } yield (t1::t2).mkString).suchThat(! _.matches("g|c|h|m|ms"))
}