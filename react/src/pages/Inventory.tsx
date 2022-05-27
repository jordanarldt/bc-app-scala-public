import { Panel, Text } from "@bigcommerce/big-design";
import React from "react";
import { InventorySection } from "../components/InventorySection";

export const Inventory = () => {
  return (
    <Panel header="Inventory">
      <Text>
        You can see all of your store&apos;s variants here, sort them, and update the inventory level.
      </Text>
      <InventorySection />
    </Panel>
  );
};
