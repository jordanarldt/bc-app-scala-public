import useSWR from "swr";
import { useSession } from "../context/SessionProvider";
import { ApiError, PaginationInfo, PermissionsResponse, StoreUser, Variant, BcMeta } from "../types";

const apiHost = process.env.NODE_ENV === "development" ? "http://localhost:8080" : "";

async function fetcher(url: string, query: string) {
  const res = await fetch(`${apiHost}${url}?${query}`);

  if (!res.ok) {
    const { message } = await res.json();
    const error: ApiError = new Error(message);
    error.status = res.status;

    throw error;
  }

  return res.json();
}

// Hook to fetch the current user role from the API
// since it won't be stored in the context and may not always be needed.
export function usePermissions() {
  const context = useSession();
  const params = new URLSearchParams({ context }).toString();
  const { data, error } = useSWR(context ? ["/api/users/role", params] : null, fetcher);

  return {
    data: data as PermissionsResponse, 
    error
  };
}

// Hook to fetch the current store users from the API
export function useStoreUsers(page: number, limit: number, search: string = "") {
  // consider adding a name filter in the api?
  const context = useSession();
  const params = new URLSearchParams({ context, page: String(page), limit: String(limit), search }).toString();

  const { data, error } = useSWR(context ? ["/api/users", params] : null, fetcher);

  return {
    users: data?.data as StoreUser[],
    pagination: data?.pagination as PaginationInfo,
    isLoading: !data && !error,
    error
  };
}

export function useVariants(page: number, limit: number, like: string = "") {
  const context = useSession();
  const params = new URLSearchParams({ context, page: String(page), limit: String(limit), like }).toString();

  const { data, error, mutate } = useSWR(context ? ["/api/variants", params] : null, fetcher);

  return {
    variants: data?.data as Variant[],
    meta: data?.meta as BcMeta,
    isLoading: !data && !error,
    error,
    mutate
  };
}
