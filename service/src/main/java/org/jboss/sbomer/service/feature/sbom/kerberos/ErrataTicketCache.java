/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.sbomer.service.feature.sbom.kerberos;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosTicket;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class ErrataTicketCache {

    // Ticket-Granting Ticket (TGT) Cache
    private final ConcurrentMap<String, SubjectTgtPair> tgtCache = new ConcurrentHashMap<>();

    public SubjectTgtPair getTgt(String userPrincipal) {
        SubjectTgtPair cached = tgtCache.get(userPrincipal);
        if (cached != null) {
            log.debug("Got Subject-TGT from the cache");
        }
        return cached;
    }

    public void invalidateTgt(String userPrincipal) {
        tgtCache.remove(userPrincipal);
    }

    public void cacheTgt(String userPrincipal, Subject subject) {
        KerberosTicket ticket = extractTgtKerberosTicket(subject);
        if (ticket != null) {
            SubjectTgtPair pair = new SubjectTgtPair(ticket, subject);
            tgtCache.put(userPrincipal, pair);
        }
    }

    public static KerberosTicket extractTgtKerberosTicket(Subject subject) {
        for (KerberosTicket ticket : subject.getPrivateCredentials(KerberosTicket.class)) {
            if (ticket.getServer().getName().startsWith("krbtgt")) {
                return ticket;
            }
        }
        return null;
    }

}
