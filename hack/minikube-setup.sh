#!/bin/env bash

#
# JBoss, Home of Professional Open Source.
# Copyright 2023 Red Hat, Inc., and individual contributors
# as indicated by the @author tags.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

set -e

# This file prepares an opinionated local Minikube enviroment.
# It uses the KVM driver and creates a 'sbomer' Minikube profile
#
# After Minikube is setup you can use 'minikube-start.sh' and 'minikube-stop.sh' scripts.
#
# See: minikube profile list

exec minikube start -p sbomer --driver=kvm2 --cpus=4 --memory=4g --disk-size=20GB --kubernetes-version=v1.25.16 --embed-certs