import React from "react";

export interface AppRoute {
  name: string;
  path: string;
  icon?: React.ReactElement;
  element: React.ReactElement;
}

export interface ApiError extends Error {
  status?: number;
}
