import React from "react";
import { Collapse, Panel, Text } from "@bigcommerce/big-design";

export const Help = () => {
  return (
    <>
      <Panel
        header="Having trouble?"
        action={{
          variant: "secondary",
          text: "Contact Support",
          onClick: () => {
            // Open modal or redirect to support page
          }
        }}
      >
        <Text>This is the help page. This is where you would want to put troubleshooting steps and helpful information for your app users.</Text>
      </Panel>
      <Panel header="Frequently Asked Questions">
        <Text>This is the FAQ page. This is where you would want to put frequently asked questions for your app users.</Text>
        <div>
          <Collapse title="Will this app give me superpowers?">
            <Text>Probably not, but the app will help you manage your BigCommerce store.</Text>
          </Collapse>
        </div>
        <div>
          <Collapse title="Why am I getting an error?">
            <Text>If you're getting an error, you're likely doing something incorrectly. Here are the steps on how to do it correctly:</Text>
            <ol>
              <li><Text as="span">Do this thing</Text></li>
              <li><Text as="span">And then this thing</Text></li>
              <li><Text as="span">And make sure of this</Text></li>
              <li><Text as="span">And then it should work</Text></li>
            </ol>
          </Collapse>
        </div>
      </Panel>
    </>
  );
};
