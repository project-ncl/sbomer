# WARNING

This package contains code that will be relocated outside of the service.
We should not make any hard dependencies on any parts of the system. Instead
eventing should be used.

Handlers are services that translate external events into internal events.