import { Panel, Text } from "@bigcommerce/big-design";
import React from "react";

export const GettingStarted = () => {
  return (
    <>
      <Panel header="Getting Started">
        <Text>
          Welcome! This page is where you could put any helpful info to introduce users to the flow of your application. This example app will demonstrate basic API functionality such as fetching product information, updating product inventory, and permissions-based user roles.
        </Text>
      </Panel>
      <Panel header="Why Scala?">
        <Text>
          Scala is not a new language by any means, but it has been gaining popularity in the last few years. Scala is a great language for building backend services and web applications that are scalable and performant due to its strong type system and functional programming features.
        </Text>
      </Panel>
      <Panel header="Technology Stack">
        <Text>
          This example app is built with <strong>Scala</strong> for the back-end, <strong>ReactJS</strong> for the front-end, and <strong>PostgreSQL</strong> for the database.
        </Text>
      </Panel>
    </>
  );
};
