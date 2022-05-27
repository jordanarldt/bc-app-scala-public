import React from "react";
import { 
  Panel, 
  Text, 
} from "@bigcommerce/big-design";
import { UsersSection } from "../components/UsersSection";

export const Users = () => {
  return (
    <Panel header="Users">
      <Text>This is a list of users that are able to use this app on your store and their permissions. By default, when a user first loads the app after being given permissions, they will only have the <strong>Viewer</strong> role. If you want them to have <strong>Admin</strong> permissions, you must set that here.</Text>
      <UsersSection />
    </Panel>
  );
};
