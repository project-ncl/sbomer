package org.jboss.sbomer.service.test.integ.feature.sbom;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.validation.ValidationException;

import org.jboss.sbomer.service.feature.sbom.pyxis.PyxisValidatingClient;
import org.jboss.sbomer.service.feature.sbom.pyxis.dto.PyxisRepositoryDetails;
import org.jboss.sbomer.service.test.PyxisWireMock;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.client.WireMock;

import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;

import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.jboss.sbomer.service.test.utils.umb.TestUmbProfile;

@QuarkusTest
@WithTestResource(PyxisWireMock.class)
@TestProfile(PyxisClientIT.class)
@Slf4j
public class PyxisClientIT extends TestUmbProfile {
    @Inject
    Validator validator;

    @Inject
    PyxisValidatingClient pc;

    @Override
    public Map<String, String> getConfigOverrides() {
        /*
         * The original annotation is
         *
         * @Retry( maxRetries = PYXIS_UNPUBLISHED_MAX_RETRIES, (18) delay = PYXIS_UNPUBLISHED_INITIAL_DELAY, (3600000)
         * maxDuration = PYXIS_UNPUBLISHED_MAX_DURATION, (68400000) retryOn = ConstraintViolationException.class)
         *
         * @BeforeRetry(RetryLogger.class)
         *
         * @FibonacciBackoff(maxDelay = PYXIS_UNPUBLISHED_MAX_DELAY) (68400000)
         */
        return Map.of(
                "Retry/maxRetries",
                "9",
                "Retry/delay",
                "30",
                "Retry/maxDuration",
                "6840",
                "Retry/maxDelay",
                "4000",
                "Retry/delayUnit",
                "millis",
                "FibonacciBackoff/maxDelay",
                "6830",
                "ExponentialBackoff/maxDelay",
                "4000",
                "Retry/maxDelayUnit",
                "millis",
                "Retry/maxDurationUnit",
                "millis",
                "Timeout/unit",
                "millis");
    }

    static final String INVALID_PAYLOAD = "{\"data\":[]}";
    static final String EXAMPLE_RETURN = """
            {
                      "data": [
                        {
                          "repositories": [
                            {
                              "registry": "registry-proxy.engineering.redhat.com",
                              "repository": "rh-osbs/jboss-eap-7-eap74-openjdk17-openshift-rhel8",
                              "tags": [
                                {
                                  "added_date": "2025-01-15T19:30:23.330000+00:00",
                                  "name": "7.4.20-9",
                                  "_links": {
                                    "tag_history": {
                                      "href": "/v1/tag-history/registry/registry-proxy.engineering.redhat.com/repository/rh-osbs/jboss-eap-7-eap74-openjdk17-openshift-rhel8/tag/7.4.20-9"
                                    }
                                  }
                                },
                                {
                                  "added_date": "2025-01-15T19:30:23.330000+00:00",
                                  "name": "jb-eap-7.4-rhel-8-containers-candidate-12226-20250115185812-aarch64",
                                  "_links": {
                                    "tag_history": {
                                      "href": "/v1/tag-history/registry/registry-proxy.engineering.redhat.com/repository/rh-osbs/jboss-eap-7-eap74-openjdk17-openshift-rhel8/tag/jb-eap-7.4-rhel-8-containers-candidate-12226-20250115185812-aarch64"
                                    }
                                  }
                                }
                              ],
                              "published": true,
                              "_links": {
                                "repository": {
                                  "href": "/v1/repositories/registry/registry-proxy.engineering.redhat.com/repository/rh-osbs/jboss-eap-7-eap74-openjdk17-openshift-rhel8"
                                }
                              }
                            }
                          ]
                        },
                        {
                          "repositories": [
                            {
                              "registry": "registry-proxy.engineering.redhat.com",
                              "repository": "rh-osbs/jboss-eap-7-eap74-openjdk17-openshift-rhel8",
                              "tags": [
                                {
                                  "added_date": "2025-01-15T19:30:15.431000+00:00",
                                  "name": "7.4.20-9",
                                  "_links": {
                                    "tag_history": {
                                      "href": "/v1/tag-history/registry/registry-proxy.engineering.redhat.com/repository/rh-osbs/jboss-eap-7-eap74-openjdk17-openshift-rhel8/tag/7.4.20-9"
                                    }
                                  }
                                },
                                {
                                  "added_date": "2025-01-15T19:30:15.431000+00:00",
                                  "name": "jb-eap-7.4-rhel-8-containers-candidate-12226-20250115185812-ppc64le",
                                  "_links": {
                                    "tag_history": {
                                      "href": "/v1/tag-history/registry/registry-proxy.engineering.redhat.com/repository/rh-osbs/jboss-eap-7-eap74-openjdk17-openshift-rhel8/tag/jb-eap-7.4-rhel-8-containers-candidate-12226-20250115185812-ppc64le"
                                    }
                                  }
                                }
                              ],
                              "published": false,
                              "_links": {
                                "repository": {
                                  "href": "/v1/repositories/registry/registry-proxy.engineering.redhat.com/repository/rh-osbs/jboss-eap-7-eap74-openjdk17-openshift-rhel8"
                                }
                              }
                            }
                          ]
                        },
                        {
                          "repositories": [
                            {
                              "registry": "registry-proxy.engineering.redhat.com",
                              "repository": "rh-osbs/jboss-eap-7-eap74-openjdk17-openshift-rhel8",
                              "tags": [
                                {
                                  "added_date": "2025-01-15T19:30:07.749000+00:00",
                                  "name": "7.4.20-9",
                                  "_links": {
                                    "tag_history": {
                                      "href": "/v1/tag-history/registry/registry-proxy.engineering.redhat.com/repository/rh-osbs/jboss-eap-7-eap74-openjdk17-openshift-rhel8/tag/7.4.20-9"
                                    }
                                  }
                                },
                                {
                                  "added_date": "2025-01-15T19:30:07.749000+00:00",
                                  "name": "jb-eap-7.4-rhel-8-containers-candidate-12226-20250115185812-x86_64",
                                  "_links": {
                                    "tag_history": {
                                      "href": "/v1/tag-history/registry/registry-proxy.engineering.redhat.com/repository/rh-osbs/jboss-eap-7-eap74-openjdk17-openshift-rhel8/tag/jb-eap-7.4-rhel-8-containers-candidate-12226-20250115185812-x86_64"
                                    }
                                  }
                                }
                              ],
                              "published": false,
                              "_links": {
                                "repository": {
                                  "href": "/v1/repositories/registry/registry-proxy.engineering.redhat.com/repository/rh-osbs/jboss-eap-7-eap74-openjdk17-openshift-rhel8"
                                }
                              }
                            }
                          ]
                        },
                        {
                          "repositories": [
                            {
                              "registry": "registry-proxy.engineering.redhat.com",
                              "repository": "rh-osbs/jboss-eap-7-eap74-openjdk17-openshift-rhel8",
                              "tags": [
                                {
                                  "added_date": "2025-01-15T19:29:58.414000+00:00",
                                  "name": "7.4.20-9",
                                  "_links": {
                                    "tag_history": {
                                      "href": "/v1/tag-history/registry/registry-proxy.engineering.redhat.com/repository/rh-osbs/jboss-eap-7-eap74-openjdk17-openshift-rhel8/tag/7.4.20-9"
                                    }
                                  }
                                },
                                {
                                  "added_date": "2025-01-15T19:29:58.414000+00:00",
                                  "name": "jb-eap-7.4-rhel-8-containers-candidate-12226-20250115185812-s390x",
                                  "_links": {
                                    "tag_history": {
                                      "href": "/v1/tag-history/registry/registry-proxy.engineering.redhat.com/repository/rh-osbs/jboss-eap-7-eap74-openjdk17-openshift-rhel8/tag/jb-eap-7.4-rhel-8-containers-candidate-12226-20250115185812-s390x"
                                    }
                                  }
                                }
                              ],
                              "published": false,
                              "_links": {
                                "repository": {
                                  "href": "/v1/repositories/registry/registry-proxy.engineering.redhat.com/repository/rh-osbs/jboss-eap-7-eap74-openjdk17-openshift-rhel8"
                                }
                              }
                            }
                          ]
                        }
                      ]
                    }
  }
                    """;
    static final String UNPUBLISHED = """
            {
                "data": [
                  {
                    "repositories": [
                      {
                        "registry": "registry-proxy.engineering.redhat.com",
                        "repository": "rh-osbs/jboss-eap-7-eap74-openjdk17-openshift-rhel8",
                        "tags": [
                          {
                            "added_date": "2025-01-15T19:30:23.330000+00:00",
                            "name": "7.4.20-9",
                            "_links": {
                              "tag_history": {
                                "href": "/v1/tag-history/registry/registry-proxy.engineering.redhat.com/repository/rh-osbs/jboss-eap-7-eap74-openjdk17-openshift-rhel8/tag/7.4.20-9"
                              }
                            }
                          },
                          {
                            "added_date": "2025-01-15T19:30:23.330000+00:00",
                            "name": "jb-eap-7.4-rhel-8-containers-candidate-12226-20250115185812-aarch64",
                            "_links": {
                              "tag_history": {
                                "href": "/v1/tag-history/registry/registry-proxy.engineering.redhat.com/repository/rh-osbs/jboss-eap-7-eap74-openjdk17-openshift-rhel8/tag/jb-eap-7.4-rhel-8-containers-candidate-12226-20250115185812-aarch64"
                              }
                            }
                          }
                        ],
                        "published": false,
                        "_links": {
                          "repository": {
                            "href": "/v1/repositories/registry/registry-proxy.engineering.redhat.com/repository/rh-osbs/jboss-eap-7-eap74-openjdk17-openshift-rhel8"
                          }
                        }
                      }
                    ]
                  },
                  {
                    "repositories": [
                      {
                        "registry": "registry-proxy.engineering.redhat.com",
                        "repository": "rh-osbs/jboss-eap-7-eap74-openjdk17-openshift-rhel8",
                        "tags": [
                          {
                            "added_date": "2025-01-15T19:30:15.431000+00:00",
                            "name": "7.4.20-9",
                            "_links": {
                              "tag_history": {
                                "href": "/v1/tag-history/registry/registry-proxy.engineering.redhat.com/repository/rh-osbs/jboss-eap-7-eap74-openjdk17-openshift-rhel8/tag/7.4.20-9"
                              }
                            }
                          },
                          {
                            "added_date": "2025-01-15T19:30:15.431000+00:00",
                            "name": "jb-eap-7.4-rhel-8-containers-candidate-12226-20250115185812-ppc64le",
                            "_links": {
                              "tag_history": {
                                "href": "/v1/tag-history/registry/registry-proxy.engineering.redhat.com/repository/rh-osbs/jboss-eap-7-eap74-openjdk17-openshift-rhel8/tag/jb-eap-7.4-rhel-8-containers-candidate-12226-20250115185812-ppc64le"
                              }
                            }
                          }
                        ],
                        "published": false,
                        "_links": {
                          "repository": {
                            "href": "/v1/repositories/registry/registry-proxy.engineering.redhat.com/repository/rh-osbs/jboss-eap-7-eap74-openjdk17-openshift-rhel8"
                          }
                        }
                      }
                    ]
                  }
                ]
              }
              """;


    void ex(String nvr, List<String> qp) {
      PyxisRepositoryDetails prd = pc.getRepositoriesDetails(nvr, qp);
      Set<ConstraintViolation<PyxisRepositoryDetails>> cvs = validator.validate(prd);
      cvs.forEach(v -> log.info(v.toString()));
    }

    List<Long> requestDeltas(List<ServeEvent> serveEvents) {
      Iterator it = serveEvents.iterator();
      List<Long> deltas = new ArrayList();

      while (it.hasNext()) {
          ServeEvent s1 = (ServeEvent) it.next();
          ServeEvent s2 = it.hasNext() ? (ServeEvent) it.next() : s1;
          delta(s2.getRequest().getLoggedDate(), s1.getRequest().getLoggedDate(), ChronoUnit.MILLIS);
          deltas.add(delta(s1.getRequest().getLoggedDate(), s2.getRequest().getLoggedDate(), ChronoUnit.MILLIS));
      }
      return deltas;
    }

    static long delta(Date e, Date o, ChronoUnit unit) {
      return unit.between(e.toInstant(), o.toInstant());
    }

    @Test
    @TestSecurity(authorizationEnabled = false)
    void testUnreleasedNVR() {
        PyxisWireMock.wireMockServer.stubFor(
                WireMock.get(
                        urlPathMatching(
                                "/v1/images/nvr/jboss-eap-74-openjdk17-builder-openshift-rhel8-container-7.4.20-UNRELEASED"))
                        .withHeader("Accept", containing("json"))
                        .willReturn(
                                WireMock.aResponse()
                                        .withBody(INVALID_PAYLOAD)
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")));

        PyxisWireMock.wireMockServer.stubFor(
                WireMock.any(anyUrl())
                        .atPriority(10)
                        .willReturn(
                                aResponse().withStatus(404)
                                        .withBody("Default Stub Return")
                                        .withHeader("Content-Type", "text/plain")));

        String nvr = "jboss-eap-74-openjdk17-builder-openshift-rhel8-container-7.4.20-UNRELEASED";
        List<String> qp = Arrays.asList(
                "data.repositories.registry",
                "data.repositories.repository",
                "data.repositories.tags",
                "data.repositories.published");

        assertThrows(ValidationException.class, () -> {
            PyxisRepositoryDetails prd = pc.getRepositoriesDetails(nvr, qp);
            Set<ConstraintViolation<PyxisRepositoryDetails>> cvs = validator.validate(prd);
        });

        List<ServeEvent> se = PyxisWireMock.wireMockServer.getAllServeEvents();
        requestDeltas(se).forEach(rd -> log.info(rd.toString()));
        PyxisWireMock.wireMockServer.verify(
                10,
                getRequestedFor(
                        urlPathMatching(
                                "/v1/images/nvr/jboss-eap-74-openjdk17-builder-openshift-rhel8-container-7.4.20-UNRELEASED")));
    }

    @Test
    @TestSecurity(authorizationEnabled = false)
    void testReleasedNVR() {
        PyxisWireMock.wireMockServer.stubFor(
                WireMock.get(
                        urlPathMatching(
                                "/v1/images/nvr/jboss-eap-74-openjdk17-builder-openshift-rhel8-container-7.4.20-RELEASED"))
                        .withHeader("Accept", containing("json"))
                        .willReturn(
                                WireMock.aResponse()
                                        .withBody(EXAMPLE_RETURN)
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")));

        PyxisWireMock.wireMockServer.stubFor(
                WireMock.any(anyUrl())
                        .atPriority(10)
                        .willReturn(
                                aResponse().withStatus(404)
                                        .withBody("Default Stub Return")
                                        .withHeader("Content-Type", "text/plain")));

        String nvr = "jboss-eap-74-openjdk17-builder-openshift-rhel8-container-7.4.20-RELEASED";
        List<String> qp = Arrays.asList(
                "data.repositories.registry",
                "data.repositories.repository",
                "data.repositories.tags",
                "data.repositories.published");
        try {
            PyxisRepositoryDetails prd = pc.getRepositoriesDetails(nvr, qp);

        } catch (Exception e) {
            PyxisWireMock.wireMockServer.findAll(RequestPatternBuilder.allRequests())
                    .forEach(request -> System.out.println(request.getAbsoluteUrl()));
        } finally {
            PyxisWireMock.wireMockServer.findAll(RequestPatternBuilder.allRequests())
                    .forEach(request -> System.out.println(request.getAbsoluteUrl()));

        }
    }

    @Test
    @TestSecurity(authorizationEnabled = false)
    void testNonApplicationError() {
        PyxisWireMock.wireMockServer.stubFor(
                WireMock.get(
                        urlPathMatching(
                                "/v1/images/nvr/jboss-eap-74-openjdk17-builder-openshift-rhel8-container-7.4.20-RELEASED500"))
                        .withHeader("Accept", containing("json"))
                        .inScenario("Network misbehaving")
                        .whenScenarioStateIs(Scenario.STARTED)
                        .willReturn(
                                WireMock.aResponse()
                                        .withBody("null")
                                        .withStatus(500)
                                        .withHeader("Content-Type", "application/json"))
                        .willSetStateTo("Returning valid"));

        PyxisWireMock.wireMockServer.stubFor(
                WireMock.get(
                        urlPathMatching(
                                "/v1/images/nvr/jboss-eap-74-openjdk17-builder-openshift-rhel8-container-7.4.20-RELEASED500"))
                        .withHeader("Accept", containing("json"))
                        .inScenario("Network misbehaving")
                        .whenScenarioStateIs("Returning valid")
                        .willReturn(
                                WireMock.aResponse()
                                        .withBody(EXAMPLE_RETURN)
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json"))
                        .willSetStateTo("Returning empty"));

        PyxisWireMock.wireMockServer.stubFor(
                WireMock.get(
                        urlPathMatching(
                                "/v1/images/nvr/jboss-eap-74-openjdk17-builder-openshift-rhel8-container-7.4.20-RELEASED500"))
                        .withHeader("Accept", containing("json"))
                        .inScenario("Network misbehaving")
                        .whenScenarioStateIs("Returning empty")
                        .willReturn(
                                WireMock.aResponse()
                                        .withBody(INVALID_PAYLOAD)
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json"))
                        .willSetStateTo("Returning valid"));

        String nvr = "jboss-eap-74-openjdk17-builder-openshift-rhel8-container-7.4.20-RELEASED500";
        List<String> qp = Arrays.asList(
                "data.repositories.registry",
                "data.repositories.repository",
                "data.repositories.tags",
                "data.repositories.published");
        this.ex(nvr, qp);
        this.ex(nvr, qp);
    }

    @Test
    @TestSecurity(authorizationEnabled = false)
    void testReallyEmpty() {
        PyxisWireMock.wireMockServer.stubFor(
                WireMock.get(
                        urlPathMatching(
                                "/v1/images/nvr/jboss-eap-74-openjdk17-builder-openshift-rhel8-container-7.4.20-EMPTY"))
                        .withHeader("Accept", containing("json"))
                        .willReturn(
                                WireMock.aResponse()
                                        .withBody("")
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")));

        PyxisWireMock.wireMockServer.stubFor(
                WireMock.any(anyUrl())
                        .atPriority(10)
                        .willReturn(
                                aResponse().withStatus(404)
                                        .withBody("Default Stub Return")
                                        .withHeader("Content-Type", "text/plain")));

        String nvr = "jboss-eap-74-openjdk17-builder-openshift-rhel8-container-7.4.20-EMPTY";
        List<String> qp = Arrays.asList(
                "data.repositories.registry",
                "data.repositories.repository",
                "data.repositories.tags",
                "data.repositories.published");

        assertThrows(IllegalArgumentException.class, () -> {
            PyxisRepositoryDetails prd = pc.getRepositoriesDetails(nvr, qp);
        });

        PyxisWireMock.wireMockServer.findAll(RequestPatternBuilder.allRequests())
                .forEach(request -> System.out.println(request.getAbsoluteUrl()));
    }

    @Test
    @TestSecurity(authorizationEnabled = false)
    void testUnpublished() {
        PyxisWireMock.wireMockServer.stubFor(
                WireMock.get(
                        urlPathMatching(
                                "/v1/images/nvr/jboss-eap-74-openjdk17-builder-openshift-rhel8-container-7.4.20-UNPUBLISHED"))
                        .withHeader("Accept", containing("json"))
                        .willReturn(
                                WireMock.aResponse()
                                        .withBody(UNPUBLISHED)
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")));

        PyxisWireMock.wireMockServer.stubFor(
                WireMock.any(anyUrl())
                        .atPriority(10)
                        .willReturn(
                                aResponse().withStatus(404)
                                        .withBody("Default Stub Return")
                                        .withHeader("Content-Type", "text/plain")));

        String nvr = "jboss-eap-74-openjdk17-builder-openshift-rhel8-container-7.4.20-UNPUBLISHED";
        List<String> qp = Arrays.asList(
                "data.repositories.registry",
                "data.repositories.repository",
                "data.repositories.tags",
                "data.repositories.published");

        assertThrows(ValidationException.class, () -> {
            this.ex(nvr, qp);
        });

        List<ServeEvent> se = PyxisWireMock.wireMockServer.getAllServeEvents();
        requestDeltas(se).forEach(rd -> log.info(rd.toString()));
        PyxisWireMock.wireMockServer.verify(
                10,
                getRequestedFor(
                        urlPathMatching(
                                "/v1/images/nvr/jboss-eap-74-openjdk17-builder-openshift-rhel8-container-7.4.20-UNPUBLISHED")));
    }
}
