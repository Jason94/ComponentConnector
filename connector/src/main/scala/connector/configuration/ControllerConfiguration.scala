package connector.configuration

import akka.actor.ActorRef

/**
 * Store the configuration for the controller behavior.
 * 
 * @param streamSourceFactory Factory method to create an actor that implements the [[connector.controller.ControllerStreamSource]] protocol.
 */
case class ControllerConfiguration(streamSourceFactory: () => ActorRef)