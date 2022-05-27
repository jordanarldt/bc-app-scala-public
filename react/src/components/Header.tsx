import { H1 } from "@bigcommerce/big-design";
import React from "react";
import { AppRoute } from "../types";
import { NavMenu } from "./NavMenu";

interface HeaderProps {
  navRoutes: AppRoute[];
}

export const Header = ({ navRoutes }: HeaderProps) => {
  return (
    <div className="header">
      <H1>Sample BigCommerce App with Scala/React</H1>
      <NavMenu routes={navRoutes} />
    </div>
  );
};
