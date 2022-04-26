import { useState } from "react";
import { 
  Panel, 
  Table, 
  Text, 
  InlineMessage, 
  ProgressCircle, 
  Flex, 
  FlexItem, 
  Select
} from "@bigcommerce/big-design";
import { SearchBar } from "../components/SearchBar";
import { usePermissions, useStoreUsers } from "../lib/hooks";
import { updateUserRole } from "../lib/api";
import { useSession } from "../context/SessionProvider";

export const Users = () => {
  return (
    <Panel header="Users">
      <Text>This is a list of users that are able to use this app on your store and their permissions. By default, when a user first loads the app after being given permissions, they will only have the <strong>Viewer</strong> role. If you want them to have <strong>Admin</strong> permissions, you must set that here.</Text>
      <UsersSection />
    </Panel>
  );
};

const UsersSection = () => {
  const context = useSession();
  const permissions = usePermissions();

  const [currentPage, setCurrentPage] = useState(1);
  const [itemsPerPage, setItemsPerPage] = useState(10);
  const itemsPerPageOptions = [10, 25, 50, 100];
  const [filterValue, setFilterValue] = useState("");
  const { 
    users, 
    pagination,
    isLoading: usersLoading, 
    error: usersError
  } = useStoreUsers(currentPage, itemsPerPage, filterValue);

  const onSearchSubmit = (value: string) => setFilterValue(value);

  const onItemsPerPageChange = (range: number) => {
    setCurrentPage(1);
    setItemsPerPage(range);
  };

  if (!context) {
    return (
      <InlineMessage
        type="error"
        header="Error"
        messages={[
          { text: "Failed to authorize your session. Please try reloading the app." }
        ]}
      />
    );
  }

  if (usersLoading) {
    return (
      <Flex justifyContent={"center"}>
        <FlexItem><ProgressCircle size="medium" /></FlexItem>
      </Flex>
    );
  }

  if (permissions.error || usersError) {
    return (
      <InlineMessage 
        type="error"
        header="Error" 
        messages={[
          { text: "There was an error loading the users data. Please try again or reload the app." },
        ]}
      />
    );
  }

  if (!users?.length && !usersLoading && !filterValue) {
    return (
      <InlineMessage 
        type="error"
        header="Error" 
        messages={[
          { text: "There are no users to display. This indicates a problem, please try reloading the app." },
        ]}
      />
    );
  }

  return (
    <>
      <SearchBar defaultValue={filterValue} onSearchSubmit={onSearchSubmit} />
      <Table 
        columns={[
          { 
            header: "Email", 
            hash: "email", 
            render: ({ email }) => email 
          },
          { 
            header: "Role", 
            hash: "role", 
            width: "200px",
            render: ({ role, userId }) => 
              <RoleCell 
                currentUserRole={permissions.data?.role} 
                role={role} 
                userId={userId}
                context={context}
              />
          },
          { 
            header: "Last Login", 
            hash: "lastLogin", 
            render: ({ lastLogin }) => new Date(lastLogin * 1000).toLocaleString() 
          },
        ]}
        pagination={{
          currentPage,
          totalItems: pagination?.totalItems ?? 0,
          onPageChange: setCurrentPage,
          itemsPerPageOptions,
          onItemsPerPageChange,
          itemsPerPage
        }}
        items={users ?? []}
      />
      {currentPage < pagination?.totalPages && <PreFetchPage targetPage={currentPage + 1} limit={itemsPerPage} />}
    </>
  );
};

interface RoleCellProps {
  userId: number;
  role: string;
  currentUserRole: string;
  context: string;
};

const RoleCell = ({ userId, role, currentUserRole, context }: RoleCellProps) => {
  const [currentRole, setCurrentRole] = useState(role);

  const updateRole = (targetRole: string) => {
    updateUserRole(userId, targetRole, context)
    .then(() => setCurrentRole(targetRole))
    .catch(err => {
      console.log(err);
      setCurrentRole(role);
    });
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

// Invisible component to pre-fetch pages for a smoother experience
interface PreFetchPageProps {
  targetPage: number;
  limit: number;
}

function PreFetchPage({ targetPage, limit }: PreFetchPageProps) {
  useStoreUsers(targetPage, limit);
  return null;
};
