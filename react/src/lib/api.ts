import { ApiError } from "../types";

const apiHost = process.env.NODE_ENV === "development" ? "http://localhost:8080" : "";

const fetcher = async (path: string, method: string, body?: Record<string, unknown>) => {
  const res = await fetch(`${apiHost}${path}`, {
    method: method,
    headers: {
      "Content-Type": "application/json"
    },
    body: body ? JSON.stringify(body) : null
  });

  if (!res.ok) {
    const { message } = await res.json();
    const error: ApiError = new Error(message);
    error.status = res.status;

    throw error;
  }

  return res.json().catch(() => null);
};

export function updateUserRole(userId: number, role: string, context: string): Promise<void> {
  return fetcher(`/api/users?context=${context}`, "PUT", { userId, role });
}

export function updateInventoryOrTrackingType(
  variantId: number, 
  productId: number,
  trackingType: string | null,
  inventoryCount: number,
  context: string
) {
  const body = { variantId, productId, trackingType, inventoryCount };

  return fetcher(`/api/variants?context=${context}`, "PUT", body);
}
