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
"use strict";

const connect = require("gulp-connect");
const fs = require("fs");
const generator = require("@antora/site-generator-default");
const { reload: livereload } =
  process.env.LIVERELOAD === "true" ? require("gulp-connect") : {};
const { series, src, watch } = require("gulp");
const yaml = require("js-yaml");

const playbookFilename = "antora-playbook-dev.yml";
const playbook = yaml.load(fs.readFileSync(playbookFilename, "utf8"));
const outputDir = (playbook.output || {}).dir || "./build/site";
const serverConfig = {
  name: "Preview Site",
  livereload,
  host: "0.0.0.0",
  port: 4000,
  root: outputDir,
};
const antoraArgs = ["--clean", "--playbook", playbookFilename];
const watchPatterns = playbook.content.sources
  .filter((source) => !source.url.includes(":"))
  .reduce((accum, source) => {
    accum.push(`./docs/antora.yml`);
    accum.push(`./docs/modules/**/*`);
    return accum;
  }, []);

function generate(done) {
  generator(antoraArgs, process.env)
    .then(() => done())
    .catch((err) => {
      console.log(err);
      done();
    });
}

function serve(done) {
  connect.server(serverConfig, function () {
    this.server.on("close", done);
    watch(watchPatterns, generate);
    if (livereload)
      watch(this.root).on("change", (filepath) =>
        src(filepath, { read: false }).pipe(livereload())
      );
  });
}

module.exports = { serve, generate, default: series(generate, serve) };
