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

import java.util.Date;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.kerberos.KerberosTicket;
import javax.security.auth.kerberos.KeyTab;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class SubjectTgtPair {

    private final KerberosTicket tgt;
    private final Subject subject;

    /*
     * Checks if the endTime of the TGT is before the current Date
     */
    public boolean isExpired() {
        try {
            synchronized (tgt) {
                return tgt.getEndTime().before(new Date());
            }
        } catch (Exception e) {
            log.error("Failed to get Kerberos ticket end time", e);
            return true;
        }
    }

    public String printDetails() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n==> KerberosTicket <==");
        sb.append("\nAuthTime: ").append(tgt.getAuthTime());
        sb.append("\nStartTime: ").append(tgt.getStartTime());
        sb.append("\nEndTime: ").append(tgt.getEndTime());
        sb.append("\nRenewTill: ").append(tgt.getRenewTill());
        sb.append("\nServer Name: ").append(tgt.getServer().getName());
        sb.append("\nServer Realm: ").append(tgt.getServer().getRealm());
        sb.append("\nSession Key Algorithm: ").append(tgt.getSessionKey().getAlgorithm());

        sb.append("\n==> Subject.KerberosTickets <==");
        for (KerberosTicket ticket : subject.getPrivateCredentials(KerberosTicket.class)) {
            sb.append("\nAuthTime: ").append(ticket.getAuthTime());
            sb.append("\nStartTime: ").append(ticket.getStartTime());
            sb.append("\nEndTime: ").append(ticket.getEndTime());
            sb.append("\nRenewTill: ").append(ticket.getRenewTill());
            sb.append("\nServer Name: ").append(ticket.getServer().getName());
            sb.append("\nServer Realm: ").append(ticket.getServer().getRealm());
            sb.append("\nSession Key Algorithm: ").append(ticket.getSessionKey().getAlgorithm());
        }

        sb.append("\n==> Subject.KerberosKeys <==");
        Set<KerberosKey> kerberosKeys = subject.getPrivateCredentials(KerberosKey.class);
        for (KerberosKey kerberosKey : kerberosKeys) {
            sb.append("\nAlgorithm: ").append(kerberosKey.getAlgorithm());
            sb.append("\nPrincipal Name: ").append(kerberosKey.getPrincipal().getName());
        }
        sb.append("\n==> Subject.KeyTabs <==");
        Set<KeyTab> keyTabs = subject.getPrivateCredentials(KeyTab.class);
        for (KeyTab keyTab : keyTabs) {
            sb.append("\nPrincipal Name: ").append(keyTab.getPrincipal().getName());
            sb.append("\nPrincipal Realm: ").append(keyTab.getPrincipal().getRealm());
        }
        return sb.toString();
    }

}