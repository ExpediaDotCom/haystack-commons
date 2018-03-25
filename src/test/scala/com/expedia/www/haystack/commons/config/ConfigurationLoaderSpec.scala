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

package com.expedia.www.haystack.commons.config

import com.expedia.www.haystack.commons.unit.UnitTestSpec

class ConfigurationLoaderSpec extends UnitTestSpec {

  "ConfigurationLoader.loadConfigFileWithEnvOverrides" should {

    "load a given config file as expected when no environment overrides are present" in {
      Given("a sample HOCON conf file")
      val file = "sample.conf"
      When("loadConfigFileWithEnvOverrides is invoked with no environment variables")
      val config = ConfigurationLoader.loadConfigFileWithEnvOverrides(resourceName = file)
      Then("it should load the configuration entries as expected")
      "influxdb.kube-system.svc" should equal(config.getString("haystack.graphite.host"))
      2003 should equal(config.getInt("haystack.graphite.port"))
    }
  }

  "ConfigurationLoader.parsePropertiesFromMap" should {
    "parses a given map and returns transformed key-value that matches a given prefix" in {
      Given("a sample map with a key-value")
      val data = Map("FOO_HAYSTACK_GRAPHITE_HOST" -> "influxdb.kube-system.svc", "foo.bar" -> "baz")
      When("parsePropertiesFromMap is invoked with matching prefix")
      val config = ConfigurationLoader.parsePropertiesFromMap(data, "FOO_")
      Then("it should transform the entries that match the prefix as expected")
      Some("influxdb.kube-system.svc") should equal(config.get("haystack.graphite.host"))
      None should be (config.get("foo.bar"))
    }
  }

}
