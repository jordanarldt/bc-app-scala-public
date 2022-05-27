import React from "react";
import ReactDOM from "react-dom";
import { GlobalStyles } from "@bigcommerce/big-design";
import { theme } from "@bigcommerce/big-design-theme";
import { createGlobalStyle, ThemeProvider } from "styled-components";
import { App } from "./App";
import "./style.css";

const AppGlobalStyles = createGlobalStyle`
  body {
    height: 100%;
    max-width: 1080px;
    min-width: 600px;
    margin: 2em auto;
    background-color: ${({ theme }) => theme.colors.secondary10};
  }

  #root {
    padding: 1em;
  }

  a {
    color: ${({ theme }) => theme.colors.primary};
  }

  a:hover {
    color: ${({ theme }) => theme.colors.primary30};
  }
`;

ReactDOM.render(
  <React.StrictMode>
    <ThemeProvider theme={theme}>
      <GlobalStyles />
      <AppGlobalStyles />
      <App />
    </ThemeProvider>
  </React.StrictMode>,
  document.getElementById("root")
);
