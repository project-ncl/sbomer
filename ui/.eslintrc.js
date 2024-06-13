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
module.exports = {
  // tells eslint to use the TypeScript parser
  "parser": "@typescript-eslint/parser",
  // tell the TypeScript parser that we want to use JSX syntax
  "parserOptions": {
    "tsx": true,
    "jsx": true,
    "js": true,
    "useJSXTextNode": true,
    "project": "./tsconfig.json",
    "tsconfigRootDir": "."
  },
  // we want to use the recommended rules provided from the typescript plugin
  "extends": [
    "@redhat-cloud-services/eslint-config-redhat-cloud-services",
    "eslint:recommended",
    "plugin:react/recommended",
    "plugin:@typescript-eslint/recommended"
  ],
  "globals": {
    "window": "readonly",
    "describe": "readonly",
    "test": "readonly",
    "expect": "readonly",
    "it": "readonly",
    "process": "readonly",
    "document": "readonly",
    "insights": "readonly",
    "shallow": "readonly",
    "render": "readonly",
    "mount": "readonly"
  },
  "overrides": [
    {
      "files": ["src/**/*.ts", "src/**/*.tsx"],
      "parser": "@typescript-eslint/parser",
      "plugins": ["@typescript-eslint"],
      "extends": ["plugin:@typescript-eslint/recommended"],
      "rules": {
        "react/prop-types": "off",
        "@typescript-eslint/no-unused-vars": "error"
      },
    },
  ],
  "settings": {
    "react": {
      "version": "^16.11.0"
    }
  },
  // includes the typescript specific rules found here: https://github.com/typescript-eslint/typescript-eslint/tree/master/packages/eslint-plugin#supported-rules
  "plugins": [
    "@typescript-eslint",
    "react-hooks",
    "eslint-plugin-react-hooks"
  ],
  "rules": {
    "sort-imports": [
      "error",
      {
        "ignoreDeclarationSort": true
      }
    ],
    "@typescript-eslint/explicit-function-return-type": "off",
    "react-hooks/rules-of-hooks": "error",
    "react-hooks/exhaustive-deps": "warn",
    "@typescript-eslint/interface-name-prefix": "off",
    "prettier/prettier": "off",
    "import/no-unresolved": "off",
    "import/extensions": "off",
    "react/prop-types": "off"
  },
  "env": {
    "browser": true,
    "node": true
  }
}
