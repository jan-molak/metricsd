package com.smartcodeltd.statsd

import scala.util.{Success, Try, Failure}


object Parser {
  def parse(metric: String): Try[Sample] = metric match {
    case gauge(name, value)                   => Success(GaugeSample(name, value.toLong))
    case counter(name, value)                 => Success(CounterSample(name, value.toLong))
    case counter_with_rate(name, value, rate) => Success(CounterSample(name, value.toLong, rate.toDouble))
    case timer(name, value)                   => Success(TimerSample(name, value.toLong))
    case histogram(name, value)               => Success(HistogramSample(name, value.toLong))
    case meter(name, value)                   => Success(MeterSample(name, value.toLong))
    case _ => Failure(new IllegalArgumentException(s"Couldn't parse: '$metric'"))
  }

  private val metricName        = "[^:]+"
  private val positiveValue     = "[0-9]+"
  private val anyValue          = "-?[0-9]+"
  private val sampleRate        = "0\\.[0-9]+"

  private val gauge             = s"^($metricName):($positiveValue)\\|g$$".r
  private val counter           = s"^($metricName):($anyValue)\\|c$$".r
  private val counter_with_rate = s"^($metricName):($anyValue)\\|c\\|@($sampleRate)$$".r
  private val timer             = s"^($metricName):($positiveValue)\\|ms$$".r
  private val histogram         = s"^($metricName):($positiveValue)\\|h$$".r
  private val meter             = s"^($metricName):($positiveValue)\\|m$$".r
}


/*
 * https://github.com/b/statsd_spec
 */

sealed trait Sample

/**
 * A gauge is an instantaneous measurement of a value, like the gas gauge in a car.
 * It differs from a counter by being calculated at the client rather than the server.
 *
 * <metric name>:<value>|g
 *
 * @param name  Unique name of the gauge
 * @param value Valid gauge values are in the range [0, 2^64^)
 */
case class GaugeSample(name: String, value: Long) extends Sample

/**
 * A counter is a gauge calculated at the server.
 * Metrics sent by the client increment or decrement the value of the gauge
 * rather than giving its current value.
 * Counters may also have an associated sample rate.
 *
 * <metric name>:<value>|c[|@<sample rate>]
 *
 * @param name  Unique name of the counter
 * @param value Valid counter values are in the range (-2^63^, 2^63^)
 * @param sampleRate Sample rate, given as a decimal of the number of samples per event count. For example, a sample rate of 1/10 would be exported as 0.1.
 */
case class CounterSample(name: String, value: Long, sampleRate: Double = 1.0) extends Sample

/**
 * A timer is a measure of the number of milliseconds elapsed between a start and end time,
 * for example the time to complete rendering of a web page for a user.
 *
 * <metric name>:<value>|ms
 *
 * @param name  Unique name of the timer
 * @param value Valid timer values are in the range [0, 2^64^).
 */
case class TimerSample(name: String, value: Long) extends Sample

/**
 * A histogram is a measure of the distribution of timer values over time, calculated at the server.
 * As the data exported for timers and histograms is the same, this is currently an alias for a timer.
 *
 * <metric name>:<value>|h
 *
 * @param name  Unique name of the histogram
 * @param value Valid histogram values are in the range [0, 2^64^).
 */
case class HistogramSample(name: String, value: Long) extends Sample

/**
 * A meter measures the rate of events over time, calculated at the server.
 * They may also be thought of as increment-only counters.
 *
 * <metric name>:<value>|m
 *
 * @param name  Unique name of the Meter
 * @param value Valid meter values are in the range [0, 2^64^).
 */
case class MeterSample(name: String, value: Long) extends Sample