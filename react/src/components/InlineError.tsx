import { InlineMessage } from "@bigcommerce/big-design";
import React from "react";

export const InlineError = ({ message }: { message: string }) => {
  return (
    <InlineMessage 
      type="error"
      header="Error" 
      messages={[{ text: message }]}
    />
  );
};
