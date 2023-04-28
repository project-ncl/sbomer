# Features and feature flags

SBOMer features are designed as individual entities which can be enabled or disabled on-demand.

All configuration is available in the `sbomer.features` section in the configuration.

## UMB

The UMB feature is responsible for the Universal Message Bus integration. It allows for consuming events from Project Newcastle (PNC) build system that could result in SBOM generation as well as sending SBOM generation completion events.

### Configuration

All configuration for the UMB feature is located under the `sbomer.features.umb` section.

Global

- `enabled`: `<true|false>` -- Controls the feature enablement. Setting it to `false` disables the feature globally and overrides any other nested `enabled` flags.

Consumer

- `consumer.enabled`: `<true|false>` -- Controls the enablement of the consumer feature
- `consumer.topic`: `<STRING>` -- Name of the topic on which the consumer should be listening
- `consumer.trigger`: `<all|product|none` -- Filter that controls which messages received on the UMB will trigger SBOM generation.

Producer

- `producer.enabled`: `<true|false>` -- Controls the enablement of the producer feature.
- `producer.topic`: `<STRING>` -- Name of the topic on which the consumer should be listening
