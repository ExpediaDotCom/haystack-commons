/*
 *  Copyright 2017 Expedia, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.expedia.www.haystack.commons.entities

import com.expedia.www.haystack.commons.unit.UnitTestSpec


class MetricPointSpec extends UnitTestSpec {

  val DURATION_METRIC_NAME = "duration"
  val SERVICE_NAME_WITH_DOT = "dummy.service.name"
  val SERVICE_NAME_WITH_SPACE = "dummy service name"
  val SERVICE_NAME_WITH_COLON = "dummy:service-name"
  val OPERATION_NAME_WITH_DOT = "dummy.operation.name"
  val OPERATION_NAME_WITH_SPACE = "dummy operation name"
  val OPERATION_NAME_WITH_COLON = "dummy:operation-name"

  "MetricPoint entity" should {

    "replace period with underscore in tag values for metric point key" in {

      Given("metric point with period in service and operation name")
      val keys = Map(TagKeys.OPERATION_NAME_KEY -> OPERATION_NAME_WITH_DOT,
        TagKeys.SERVICE_NAME_KEY -> SERVICE_NAME_WITH_DOT)
      val metricPoint = MetricPoint(DURATION_METRIC_NAME, MetricType.Gauge, keys, 80, computeCurrentTimeInSecs)

      When("we get the metric point key with config enabled")
      val metricPointKey = metricPoint.getMetricPointKey(true, false)

      Then("metric point key should have value with period replaced with underscore")
      metricPointKey shouldEqual
        "haystack." + TagKeys.OPERATION_NAME_KEY + "." + OPERATION_NAME_WITH_DOT.replace(".", "___") + "." +
          TagKeys.SERVICE_NAME_KEY + "." + SERVICE_NAME_WITH_DOT.replace(".", "___") + "." +
          DURATION_METRIC_NAME
    }

    "should not replace period with underscore in tag values for metric point key" in {

      Given("metric point with period in service and operation name")
      val keys = Map(TagKeys.OPERATION_NAME_KEY -> OPERATION_NAME_WITH_DOT,
        TagKeys.SERVICE_NAME_KEY -> SERVICE_NAME_WITH_DOT)
      val metricPoint = MetricPoint(DURATION_METRIC_NAME, MetricType.Gauge, keys, 80, computeCurrentTimeInSecs)

      When("we get the metric point key with config disabled")
      val metricPointKey = metricPoint.getMetricPointKey(false, false)

      Then("metric point key should have value with period replaced with underscore")
      metricPointKey shouldEqual
        "haystack." + TagKeys.OPERATION_NAME_KEY + "." + OPERATION_NAME_WITH_DOT + "." +
          TagKeys.SERVICE_NAME_KEY + "." + SERVICE_NAME_WITH_DOT + "." +
          DURATION_METRIC_NAME
    }

    "should base64 encode service and operation name without period replacement" in {

      Given("metric point with period in service and operation name")
      val keys = Map(TagKeys.OPERATION_NAME_KEY -> OPERATION_NAME_WITH_DOT,
        TagKeys.SERVICE_NAME_KEY -> SERVICE_NAME_WITH_DOT,
        TagKeys.INTERVAL_KEY -> "FiveMinute",
        TagKeys.STATS_KEY -> "*_95")
      val metricPoint = MetricPoint(DURATION_METRIC_NAME, MetricType.Gauge, keys, 80, computeCurrentTimeInSecs)
      When("we get the metric point key with proper config")
      val metricPointKey = metricPoint.getMetricPointKey(false, true)

      Then("metric point key should have value with period replaced with underscore")
      metricPointKey shouldEqual
        "haystack." + TagKeys.OPERATION_NAME_KEY + ".ZHVtbXkub3BlcmF0aW9uLm5hbWU_." +
          TagKeys.SERVICE_NAME_KEY + ".ZHVtbXkuc2VydmljZS5uYW1l.interval.FiveMinute.stat.*_95." +
          DURATION_METRIC_NAME
    }

    "should base64 encode service and operation name with period replacement" in {

      Given("metric point with period in service and operation name")
      val keys = Map(TagKeys.OPERATION_NAME_KEY -> OPERATION_NAME_WITH_DOT,
        TagKeys.SERVICE_NAME_KEY -> SERVICE_NAME_WITH_DOT,
        TagKeys.INTERVAL_KEY -> "FiveMinute",
        TagKeys.STATS_KEY -> "*_95")
      val metricPoint = MetricPoint(DURATION_METRIC_NAME, MetricType.Gauge, keys, 80, computeCurrentTimeInSecs)

      When("we get the metric point key with proper config")
      val metricPointKey = metricPoint.getMetricPointKey(true, true)

      Then("metric point key should have value with period replaced with underscore")
      metricPointKey shouldEqual
        "haystack." + TagKeys.OPERATION_NAME_KEY + ".ZHVtbXlfX19vcGVyYXRpb25fX19uYW1l." +
          TagKeys.SERVICE_NAME_KEY + ".ZHVtbXlfX19zZXJ2aWNlX19fbmFtZQ__.interval.FiveMinute.stat.*_95." +
          DURATION_METRIC_NAME
    }

    "not do anything to colon in operation name" in {

      Given("metric point with operation name consisting of colon")
      val keys = Map(TagKeys.OPERATION_NAME_KEY -> OPERATION_NAME_WITH_COLON,
        TagKeys.SERVICE_NAME_KEY -> SERVICE_NAME_WITH_COLON)
      val metricPoint = MetricPoint(DURATION_METRIC_NAME, MetricType.Gauge, keys, 80, computeCurrentTimeInSecs)

      When("we get the metric point key")
      val metricPointKey = metricPoint.getMetricPointKey(true, false)

      Then("metric point key should have value with only period replaced with underscore and colon retained")
      metricPointKey shouldEqual
        "haystack." + TagKeys.OPERATION_NAME_KEY + "." + OPERATION_NAME_WITH_COLON.replace(".", "___") + "." +
          TagKeys.SERVICE_NAME_KEY + "." + SERVICE_NAME_WITH_COLON.replace(".", "___") + "." +
          DURATION_METRIC_NAME
    }
  }
}
