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

import org.scalatest.{FunSpec, Matchers}

class ConfigurationLoaderSpec extends FunSpec with Matchers {

  describe("configuration loader") {
    val keyName = "traces.key.sequence"

    it ("should load config from env variable with empty array value") {
      val envVars = Map[String, String](ConfigurationLoader.ENV_NAME_PREFIX + "TRACES_KEY_SEQUENCE" -> "[]")
      val config = ConfigurationLoader.loadFromEnv(envVars, Set(keyName), ConfigurationLoader.ENV_NAME_PREFIX)
      config.getList(keyName).size() shouldBe 0
    }

    it ("should load config from env variable with non-empty array value") {
      val envVars = Map[String, String](ConfigurationLoader.ENV_NAME_PREFIX + "TRACES_KEY_SEQUENCE" -> "[v1]")
      val config = ConfigurationLoader.loadFromEnv(envVars, Set(keyName), ConfigurationLoader.ENV_NAME_PREFIX)
      config.getStringList(keyName).size() shouldBe 1
      config.getStringList(keyName).get(0) shouldBe "v1"
    }

    it ("should throw runtime exception if env variable doesn't comply array value signature - [..]") {
      val envVars = Map[String, String](ConfigurationLoader.ENV_NAME_PREFIX + "TRACES_KEY_SEQUENCE" -> "v1")
      val exception = intercept[RuntimeException] {
        ConfigurationLoader.loadFromEnv(envVars, Set(keyName), ConfigurationLoader.ENV_NAME_PREFIX)
      }

      exception.getMessage shouldEqual "config key is of array type, so it should start and end with '[', ']' respectively"
    }

    it ("should load config from env variable with non-empty value") {
      val envVars = Map[String, String](
        ConfigurationLoader.ENV_NAME_PREFIX + "TRACES_KEY_SEQUENCE" -> "[v1]",
        ConfigurationLoader.ENV_NAME_PREFIX + "TRACES_KEY2" -> "v2",
        "NON_HAYSTACK_KEY" -> "not_interested")

      val config = ConfigurationLoader.loadFromEnv(envVars, Set(keyName), ConfigurationLoader.ENV_NAME_PREFIX)
      config.getStringList(keyName).size() shouldBe 1
      config.getStringList(keyName).get(0) shouldBe "v1"
      config.getString("traces.key2") shouldBe "v2"
      config.hasPath("non.haystack.key") shouldBe false
    }
  }
}
