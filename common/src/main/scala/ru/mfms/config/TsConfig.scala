package ru.mfms.config

import com.typesafe.config.{Config, ConfigFactory}

trait TsConfig {
    val config: Config = ConfigFactory.load
}
