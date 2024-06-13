import React from "react";
import ReactDOM from "react-dom/client";
import App from '@app/index';
import { FlagProvider } from '@unleash/proxy-client-react';

if (process.env.NODE_ENV !== "production") {
  const config = {
    rules: [
      {
        id: 'color-contrast',
        enabled: false
      }
    ]
  };
  // eslint-disable-next-line @typescript-eslint/no-var-requires, no-undef
  const axe = require("react-axe");
  axe(React, ReactDOM, 1000, config);
}

const config = {
  url: '<unleash-url>/api/frontend', // Your front-end API URL or the Unleash proxy's URL (https://<proxy-url>/proxy)
  clientKey: '<your-token>', // A client-side API token OR one of your proxy's designated client keys (previously known as proxy secrets)
  refreshInterval: 15, // How often (in seconds) the client should poll the proxy for updates
  appName: 'your-app-name', // The name of your application. It's only used for identifying your application
};

const root = ReactDOM.createRoot(document.getElementById("root") as Element);

root.render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
)
