import { useState, useEffect } from "react";
import { 
  Text, 
  ProgressCircle,
  Box,
  Table, 
  Flex, 
  FlexItem,
  Button,
} from "@bigcommerce/big-design";
import { SearchBar } from "./SearchBar";
import { useSession } from "../context/SessionProvider";
import { usePermissions, useVariants } from "../lib/hooks";
import { spawnVariantEditModal } from "../components/VariantEditModal";
import { InlineError } from "../components/InlineError";

export const InventorySection = () => {
  const context = useSession();
  const permissions = usePermissions();

  const [currentPage, setCurrentPage] = useState(1);
  const [itemsPerPage, setItemsPerPage] = useState(50);
  const itemsPerPageOptions = [50, 100, 200, 250];
  const [filterValue, setFilterValue] = useState("");
  const { 
    variants, 
    meta,
    isLoading: variantsLoading, 
    error: variantsError,
    mutate
  } = useVariants(currentPage, itemsPerPage, filterValue);

  const [sortedVariants, setSortedVariants] = useState(variants);
  const [columnHash, setColumnHash] = useState("");
  const [direction, setDirection] = useState<"ASC" | "DESC">("ASC");

  const onSort = (newColumnHash: string, newDirection: "ASC" | "DESC") => {
    setColumnHash(newColumnHash);
    setDirection(newDirection);
    setSortedVariants(sortVariants);
  };

  const sortVariants = sortedVariants?.sort((a, b) => {
      if (columnHash === "inventory_level") {
        return direction === "ASC" ? a[columnHash] - b[columnHash] : b[columnHash] - a[columnHash];
      } else {
        return direction === "ASC" ? a.sku.localeCompare(b.sku) : b.sku.localeCompare(a.sku);
      }
    }
  );

  const onSearchSubmit = (value: string) => setFilterValue(value);

  const onItemsPerPageChange = (range: number) => {
    setCurrentPage(1);
    setItemsPerPage(range);
  };

  const onPageChange = (page: number) => {
    setCurrentPage(page);
    // Ensure sorting is maintained on page change
    setSortedVariants(sortVariants);
  };

  useEffect(() => setSortedVariants(variants), [variants]);

  if (!context) {
    return (
      <InlineError message="Failed to authorize your session. Please try reloading the app." />
    );
  }

  if (variantsLoading) {
    return (
      <Flex justifyContent={"center"}>
        <FlexItem><ProgressCircle size="medium" /></FlexItem>
      </Flex>
    );
  }

  if (permissions.error || variantsError) {
    return (
      <InlineError message="Failed to authorize the request for the variants data. Please try reloading or reinstalling the app." />
    );
  }

  if (!variants?.length && !variantsLoading && !filterValue) {
    return (
      <InlineError message="There are no variants to display. This indicates a problem, please try reloading the app." />
    );
  }

  return (
    <>
      <SearchBar defaultValue={filterValue} onSearchSubmit={onSearchSubmit} />
      <Table 
        emptyComponent={<Box padding="medium"><Text>No results!</Text></Box>}
        columns={[
          {
            header: "Product Name",
            hash: "product_name",
            render: ({ product_name }) => product_name,
            isSortable: false,
          },
          {
            header: "Tracking Type",
            hash: "inventory_tracking",
            render: ({ inventory_tracking }) => inventory_tracking,
            isSortable: false,
          },
          {
            header: "SKU",
            hash: "sku",
            render: ({ sku }) => sku,
            isSortable: true,
          },
          {
            header: "Inventory Count",
            hash: "inventory_level",
            render: ({ inventory_level }) => inventory_level,
            isSortable: true,
          },
          {
            header: "Action",
            hideHeader: true,
            hash: "action",
            render: ({ 
              id, 
              sku, 
              product_id: productId, 
              inventory_tracking: trackingType,
              inventory_level: inventoryCount,
            }) =>
              <Button 
                variant="subtle" 
                onClick={() => spawnVariantEditModal(id, sku, productId, trackingType, inventoryCount, context, mutate)}
                disabled={permissions.data.role === "viewer"}
              >
                Edit
              </Button>,
            isSortable: false,
          }
        ]}
        pagination={{
          currentPage,
          totalItems: meta.pagination?.total ?? 0,
          onPageChange,
          itemsPerPageOptions,
          onItemsPerPageChange,
          itemsPerPage
        }}
        items={sortedVariants ?? []}
        stickyHeader={true}
        sortable={{
          columnHash,
          direction,
          onSort
        }}
      />
      {currentPage < meta?.pagination.total_pages && <PreFetchPage targetPage={currentPage + 1} limit={itemsPerPage} />}
    </>
  );
};

// Invisible component to pre-fetch pages for a smoother experience
interface PreFetchPageProps {
  targetPage: number;
  limit: number;
}

function PreFetchPage({ targetPage, limit }: PreFetchPageProps) {
  useVariants(targetPage, limit);
  return null;
};
