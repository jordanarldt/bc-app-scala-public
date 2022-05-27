import { AssignmentIcon, BaselineHelpIcon } from "@bigcommerce/big-design-icons";
import React from "react";
import { BrowserRouter, Route, Routes } from "react-router-dom";
import { Header } from "./components/Header";
import { InventoryIcon } from "./components/icons/InventoryIcon";
import { UsersIcon } from "./components/icons/UsersIcon";
import { SessionProvider } from "./context/SessionProvider";
import { GettingStarted } from "./pages/GettingStarted";
import { Help } from "./pages/Help";
import { Inventory } from "./pages/Inventory";
import { NotFound } from "./pages/NotFound";
import { Users } from "./pages/Users";
import { AppRoute } from "./types";

const appRoutes: AppRoute[] = [
  { 
    name: "Getting Started", 
    path: "/",
    icon: <AssignmentIcon />,
    element: <GettingStarted /> 
  },
  { 
    name: "Inventory", 
    path: "/inventory", 
    icon: <InventoryIcon />,
    element: <Inventory />
  },
  { 
    name: "Users", 
    path: "/users", 
    icon: <UsersIcon />,
    element: <Users /> 
  },
  {
    name: "Help",
    path: "/help",
    icon: <BaselineHelpIcon />,
    element: <Help />
  }
];

export function App(): JSX.Element {
  return (
    <BrowserRouter>
      <Header navRoutes={appRoutes} />
      <SessionProvider>
        <Routes>
          {appRoutes.map(({ path, element }, index) => 
            <Route key={`route-${index}`} path={path} element={element} />
          )}
          <Route path="*" element={<NotFound />} />
        </Routes>
      </SessionProvider>
    </BrowserRouter>
  );
}
