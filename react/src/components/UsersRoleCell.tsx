import { Select } from "@bigcommerce/big-design";
import { useState } from "react";
import { updateUserRole } from "../lib/api";

interface UsersRoleCellProps {
  userId: number;
  role: string;
  currentUserRole: string;
  context: string;
}

export const UsersRoleCell = ({ userId, role, currentUserRole, context }: UsersRoleCellProps) => {
  const [currentRole, setCurrentRole] = useState(role);

  const updateRole = async (targetRole: string) => {
    try {
      await updateUserRole(userId, targetRole, context);
      setCurrentRole(targetRole);
    } catch (e) {
      console.log(e);
      setCurrentRole(role);
    }
  };

  return (
    <Select 
      onOptionChange={updateRole}
      options={[
        { value: "admin", content: "Admin" },
        { value: "viewer", content: "Viewer" }
      ]}
      filterable={false}
      value={currentRole}
      placeholder={role === "owner" ? "Owner" : undefined}
      disabled={role === "owner" || currentUserRole === "viewer"}
    />
  );
};
