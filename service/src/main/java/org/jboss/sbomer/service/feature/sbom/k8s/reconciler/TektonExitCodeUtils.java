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
package org.jboss.sbomer.service.feature.sbom.k8s.reconciler;

public class TektonExitCodeUtils {

    public enum UnixSignal {
        SIGHUP(1, "SIGHUP", "Hangup: process disconnected or terminal closed"),
        SIGINT(2, "SIGINT", "Interrupt: interrupted by user"),
        SIGQUIT(3, "SIGQUIT", "Quit: terminated by user with core dump"),
        SIGABRT(6, "SIGABRT", "Abort: process aborted, likely internal error"),
        SIGKILL(9, "SIGKILL", "Killed: forcefully terminated, likely OOMKilled"),
        SIGSEGV(11, "SIGSEGV", "Segmentation fault: invalid memory access"),
        SIGTERM(15, "SIGTERM", "Terminate: gracefully asked to terminate");

        private final int signalNumber;
        private final String name;
        private final String description;

        UnixSignal(int signalNumber, String name, String description) {
            this.signalNumber = signalNumber;
            this.name = name;
            this.description = description;
        }

        public int getSignalNumber() {
            return signalNumber;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public static UnixSignal fromCode(int code) {
            for (UnixSignal signal : values()) {
                if (signal.signalNumber == code) {
                    return signal;
                }
            }
            return null;
        }
    }

    public static String interpretExitCode(int exitCode) {
        if (exitCode == 0) {
            return "Success";
        }

        if (exitCode > 128) {
            int signal = exitCode - 128;
            UnixSignal unixSignal = UnixSignal.fromCode(signal);
            String signalDesc = unixSignal != null ? unixSignal.getDescription() : "Unknown signal";

            return String.format("Terminated by signal %d (%s)", signal, signalDesc);
        }

        return "Exited with code " + exitCode;
    }

}