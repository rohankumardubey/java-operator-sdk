package io.javaoperatorsdk.operator.processing.event.source.controller;

import io.javaoperatorsdk.operator.processing.event.Event;
import io.javaoperatorsdk.operator.processing.event.ResourceID;

public class ResourceEvent extends Event {

  private final ResourceAction action;

  public ResourceEvent(ResourceAction action,
      ResourceID resourceID) {
    super(resourceID);
    this.action = action;
  }

  @Override
  public String toString() {
    return "ResourceEvent{" +
        "action=" + action +
        ", associated resource id=" + getRelatedCustomResourceID() +
        '}';
  }

  public ResourceAction getAction() {
    return action;
  }

}
