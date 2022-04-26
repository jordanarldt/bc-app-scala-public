package com.example

import com.example.lib.{BigCommerce, BcAuthBypass}

import akka.actor.typed.ActorSystem

// Object to handle the BigCommerce application.conf params
object AppConfig {
  def apply()(implicit system: ActorSystem[_]): (BigCommerce, Option[BcAuthBypass]) = {
    val envMode = sys.env.getOrElse("MODE", "DEV")

    // Set up configuration variables
    val configClientId = system.settings.config.getString("my-app.bigcommerce.client-id")
    val configClientSecret = system.settings.config.getString("my-app.bigcommerce.client-secret")
    val configCallback = system.settings.config.getString("my-app.bigcommerce.callback")

    val configAuthBypass = system.settings.config.getBoolean("my-app.bigcommerce.bypass-auth")
    val configBypassToken = system.settings.config.getString("my-app.bigcommerce.bypass-auth-token")
    val configBypassHash = system.settings.config.getString("my-app.bigcommerce.bypass-auth-hash")
    val configBypassUserId = system.settings.config.getInt("my-app.bigcommerce.bypass-auth-userid")

    val bcClientBypass: Option[BcAuthBypass] = 
      if (configAuthBypass && envMode == "DEV") 
        Some(BcAuthBypass(configBypassToken, configBypassHash, configBypassUserId)) 
      else 
        None

    // Pull from environment variables if available, otherwise pull from the application.conf file
    val clientId = sys.env.getOrElse("client_id", configClientId)
    val clientSecret = sys.env.getOrElse("client_secret", configClientSecret)
    val callback = sys.env.getOrElse("callback", configCallback)

    // Create the BigCommerce client for the app
    // Use it implicitly to pass to routes so it only needs
    // to be declared once.
    val bigCommerce = new BigCommerce(
      clientId = Some(clientId),
      secret = Some(clientSecret),
      callback = Some(callback)
    )

    bigCommerce -> bcClientBypass
  }
}
