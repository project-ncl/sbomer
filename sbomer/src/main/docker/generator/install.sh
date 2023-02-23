#!/bin/bash

function install_maven() {
  rm -rf maven
  mkdir maven

  pushd maven || exit

  for version in 3.6.3 3.8.7 3.9.0; do

    rm -rf apache-maven-${version}
    curl -L https://dlcdn.apache.org/maven/maven-3/${version}/binaries/apache-maven-${version}-bin.tar.gz -o maven.tar.gz
    tar -xf maven.tar.gz
    rm -f maven.tar.gz
    mv apache-maven-${version} apache-maven-${version%.*}
  done

  popd || exit
}

function install_java() {
  curl -s "https://get.sdkman.io" | bash
  source .sdkman/bin/sdkman-init.sh
  # sdk install java 8.0.362-tem
  # sdk install java 11.0.18-tem
  sdk install java 17.0.6-tem
  # sdk install java 8 "$(sdk home java 8.0.362-tem)"
  # sdk install java 11 "$(sdk home java 11.0.18-tem)"
  sdk install java 17 "$(sdk home java 17.0.6-tem)"
}

function install_domino() {
  curl -L https://github.com/quarkusio/quarkus-platform-bom-generator/releases/download/0.0.77/domino.jar -o domino.jar
}

function install_cert() {
  curl -L https://password.corp.redhat.com/RH-IT-Root-CA.crt -o RH-IT-Root-CA.crt
  keytool -import -trustcacerts -alias redhat-ca -file RH-IT-Root-CA.crt -cacerts -noprompt -storepass changeit
}

# install_maven
install_java
install_cert
install_domino
