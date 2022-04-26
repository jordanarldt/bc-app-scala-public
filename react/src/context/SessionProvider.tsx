import { createContext, useContext, useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { bcSDK } from "../scripts/bigcommerce";

const UserContext = createContext("");

export const SessionProvider = ({ children }: any) => {
  const [searchParams] = useSearchParams();
  const [context, setContext] = useState("");

  useEffect(() => {
    const query = searchParams.get("context");
    if (query) {
      setContext(query);
      bcSDK(query);
    }
  }, [searchParams]);

  return (
    <UserContext.Provider value={context}>
      {children}
    </UserContext.Provider>
  );
};

export const useSession = () => useContext(UserContext);
