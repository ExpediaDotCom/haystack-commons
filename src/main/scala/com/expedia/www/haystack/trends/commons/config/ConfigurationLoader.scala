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
package com.expedia.www.haystack.trends.commons.config

import java.io.File

import com.typesafe.config.{Config, ConfigFactory, ConfigRenderOptions}
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

object ConfigurationLoader {

  private val ENV_NAME_PREFIX = "HAYSTACK_PROP_"

  private val LOGGER = LoggerFactory.getLogger(ConfigurationLoader.getClass)

  /**
    * Loads a HOCON config file and overrides entries in them with values, if specified, from environment variables
    * with given prefix
    * For example, if the envNamePrefix given is HAYSTACK_ and if an environment variable exist with the name
    * HAYSTACK_KAFKA_STREAMS_NUM_STREAM_THREADS, then a config entry with key kafka.streams.num.stream.threads
    * will be added to the config object or an existing value in the config object will be overwritten
    *
    * @param resourceName name of the resource file to be loaded. Default value is `config/base.conf`
    * @param envNamePrefix env variable prefix to override config values. Default is `HAYSTACK_PROP_`
    *
    * @return an instance of com.typesafe.Config
    */
  def loadConfigFileWithEnvOverrides(resourceName : String = "config/base.conf", envNamePrefix : String = ENV_NAME_PREFIX) : Config = {
    val baseConfig = ConfigFactory.load(resourceName)

    val config = sys.env.get("HAYSTACK_OVERRIDES_CONFIG_PATH") match {
      case Some(path) => ConfigFactory.parseFile(new File(path)).withFallback(baseConfig)
      case _ => parsePropertiesFromMap(sys.env, envNamePrefix).withFallback(baseConfig)
    }

    LOGGER.info(config.root().render(ConfigRenderOptions.defaults().setOriginComments(false)))

    config
  }

  /**
    * Converts a Map[String, String] to HOCON Config Object
    * Filters only entries in the map where the keys start with the given prefix, removes the prefix
    * and converts the key to HOCON dot notation.  For example, if the key is HAYSTACK_KAFKA_STREAMS_NUM_STREAM_THREADS
    * and the prefix given is HAYSTACK_ then the new key will be kafka.streams.num.stream.threads
    */
  def parsePropertiesFromMap(data: Map[String, String], prefix: String) : Config = {
    val map = data.filter {
      case (envName, _) => envName.startsWith(prefix)
    } map {
      case (envName, envValue) => (transformName(envName, prefix), envValue)
    }

    ConfigFactory.parseMap(map.asJava)
  }

  /**
    * Converts a name to HOCON format removes the prefix given
    * for e.g. if the name given is HAYSTACK_KAFKA_STREAMS_NUM_STREAM_THREADS and
    * prefix is HAYSTACK_ then the return string will be kafka.streams.num.stream.threads
    */
  private def transformName(name: String, prefix: String): String = name.replaceFirst(prefix, "").toLowerCase.replace("_", ".")

}
