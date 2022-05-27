import React from "react";
import { InlineMessage } from "@bigcommerce/big-design";

export const InlineError = ({ message }: { message: string }) => {
  return (
    <InlineMessage 
      type="error"
      header="Error" 
      messages={[{ text: message }]}
    />
  );
};
