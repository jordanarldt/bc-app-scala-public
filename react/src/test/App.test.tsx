import { theme } from "@bigcommerce/big-design-theme";
import "@testing-library/jest-dom";
import { render, screen } from "@testing-library/react";
import React from "react";
import { ThemeProvider } from "styled-components";
import { App } from "../App";

describe("App", () => {
  it("renders correctly", () => {
    render(
      <ThemeProvider theme={theme}>
        <App /> 
      </ThemeProvider>
    );

    expect(screen.getByText("Sample BigCommerce App with Scala/React")).toBeInTheDocument();
  });
});
