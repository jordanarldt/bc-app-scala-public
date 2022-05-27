import { useState } from "react";
import { 
  Table, 
  ProgressCircle, 
  Flex, 
  FlexItem, 
} from "@bigcommerce/big-design";
import { SearchBar } from "../components/SearchBar";
import { usePermissions, useStoreUsers } from "../lib/hooks";
import { useSession } from "../context/SessionProvider";
import { InlineError } from "../components/InlineError";
import { UsersRoleCell } from "./UsersRoleCell";

export const UsersSection = () => {
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
      <InlineError message="Failed to authorize your session. Please try reloading the app." />
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
      <InlineError  message="There was an error loading the users data. Please try again or reload the app." />
    );
  }

  if (!users?.length && !usersLoading && !filterValue) {
    return (
      <InlineError message="There are no users to display. This indicates a problem, please try reloading the app." />
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
              <UsersRoleCell 
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

// Invisible component to pre-fetch pages for a smoother experience
interface PreFetchPageProps {
  targetPage: number;
  limit: number;
}

function PreFetchPage({ targetPage, limit }: PreFetchPageProps) {
  useStoreUsers(targetPage, limit);
  return null;
};
