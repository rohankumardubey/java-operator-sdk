---
title: Patterns and Best Practices
description: Patterns and Best Practices Implementing a Controller
layout: docs
permalink: /docs/patterns-best-practices
---

# Patterns and Best Practices

This document describes patterns and best practices, to build and run operators, and how to implement them in terms of
Java Operator SDK.

See also best practices in [Operator SDK](https://sdk.operatorframework.io/docs/best-practices/best-practices/).

## Implementing a Reconciler

### Reconcile All The Resources All the Time

The reconciliation can be triggered by events from multiple sources. It could be tempting to check the events and
reconcile just the related resource or subset of resources that the controller manages. However, this is **considered as
an anti-pattern** in operators. If triggered, all resources should be reconciled. Usually this means only comparing the
target state with the current state in the cache for most of the resource. The reason behind this is events not reliable
In general, this means events can be lost. In addition to that the operator can crash and while down will miss events.

In addition to that such approach might even complicate implementation logic in the `Reconciler`, since parallel
execution of the reconciler is not allowed for the same custom resource, there can be multiple events received for the
same resource or dependent resource during an ongoing execution, ordering those events could be also challenging.

Since there is a consensus regarding this in the industry, from v2 the events are not even accessible for
the `Reconciler`.

### EventSources and Caching

As mentioned above during a reconciliation best practice is to reconcile all the dependent resources managed by the
controller. This means that we want to compare a target state with the actual state of the cluster. Reading the actual
state of a resource from the Kubernetes API Server directly all the time would mean a significant load. Therefore, it's
a common practice to instead create a watch for the dependent resources and cache their latest state. This is done
following the Informer pattern. In Java Operator SDK, informer is wrapped into an EventSource, to integrate it with the
eventing system of the framework, resulting in `InformerEventSource`.

A new event that triggers the reconciliation is propagated when the actual resource is already in cache. So in
reconciler what should be just done is to compare the target calculated state of a dependent resource of the actual
state from the cache of the event source. If it is changed or not in the cache it needs to be created, respectively
updated.

### Idempotency

Since all the resources are reconciled during an execution and an execution can be triggered quite often, also retries
of a reconciliation can happen naturally in operators, the implementation of a `Reconciler`
needs to be idempotent. Luckily, since operators are usually managing already declarative resources, this is trivial to
do in most cases.

### Sync or Async Way of Resource Handling

In an implementation of reconciliation there can be a point when reconciler needs to wait a non-insignificant amount of
time while a resource gets up and running. For example, reconciler would do some additional step only if a Pod is ready
to receive requests. This problem can be approached in two ways synchronously or asynchronously.

The async way is just return from the reconciler, if there are informers properly in place for the target resource,
reconciliation will be triggered on change. During the reconciliation the pod can be read from the cache of the informer
and a check on it's state can be conducted again. The benefit of this approach is that it will free up the thread, so it
can be used to reconcile other resources.

The sync way would be to periodically poll the cache of the informer for the pod's state, until the target state is
reached. This would block the thread until the state is reached, which in some cases could take quite long.

## Why to Have Automated Retries?

Automatic retries are in place by default, it can be fine-tuned, but in general it's not advised to turn of automatic
retries. One of the reasons is that issues like network error naturally happen and are usually solved by a retry.
Another typical situation is for example when a dependent resource or the custom resource is updated, during the update
usually there is optimistic version control in place. So if someone updated the resource during reconciliation, maybe
using `kubectl` or another process, the update would fail on a conflict. A retry solves this problem simply by executing
the reconciliation again.

## Managing State

When managing only kubernetes resources an explicit state is not necessary about the resources. The state can be
read/watched, also filtered using labels. Or just following some naming convention. However, when managing external
resources, there can be a situation for example when the created resource can only be addressed by an ID generated when
the resource was created. This ID needs to be stored, so on next reconciliation it could be used to addressing the
resource. One place where it could go is the status sub-resource. On the other hand by definition status should be just
the result of a reconciliation. Therefore, it's advised in general, to put such state into a separate resource usually a
Kubernetes Secret or ConfigMap or a dedicated CustomResource, where the structure can be also validated.

