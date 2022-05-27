import { useState } from "react";
import ReactDOM from "react-dom";
import { 
  Modal,
  Form,
  FormGroup,
  Counter,
  Select,
  Flex,
  FlexItem,
  ProgressCircle,
} from "@bigcommerce/big-design";
import { updateInventoryOrTrackingType } from "../lib/api";
import { InlineError } from "./InlineError";

interface VariantEditModalProps {
  id: number;
  sku: string;
  productId: number;
  trackingType: string;
  inventoryCount: number;
  context: string;
  closeModal: () => void;
  onSuccess: () => void;
}

const VariantEditModal = ({
  id, 
  sku, 
  productId, 
  trackingType, 
  inventoryCount,
  context,
  closeModal,
  onSuccess,
}: VariantEditModalProps) => {
  const [inventoryValue, setInventoryValue] = useState(inventoryCount);
  const [trackingValue, setTrackingValue] = useState(trackingType);
  const [isSaving, setIsSaving] = useState(false);
  const [saveError, setSaveError] = useState(false);

  const handleSave = async () => {
    // Make sure the values have been changed, otherwise close the modal without making a request
    if (trackingValue !== trackingType || inventoryValue !== inventoryCount) {
      setIsSaving(true);
      const newTracking = trackingValue !== trackingType ? trackingValue : null;

      try {
        await updateInventoryOrTrackingType(id, productId, newTracking, inventoryValue, context);
        setIsSaving(false);
        onSuccess();
        closeModal();
      } catch {
        setIsSaving(false);
        setSaveError(true);
      }
    } else {
      closeModal();
    }
  };

  return (
    <Modal 
      actions={[
        { text: "Cancel", variant: "subtle", onClick: closeModal },
        { text: "Save", onClick: handleSave }
      ]}
      header={`Editing SKU: ${sku}`}
      isOpen={true}
      onClose={closeModal}
      closeOnEscKey={true}
      closeOnClickOutside={true}
    >
      {isSaving ? (
        <Flex justifyContent={"center"}>
          <FlexItem><ProgressCircle size="medium" /></FlexItem>
        </Flex>
      ) : (
        <Form marginBottom="xSmall">
          <FormGroup>
            <Counter
              label="Inventory Count"
              value={inventoryValue}
              min={0}
              max={1000000}
              maxLength={7}
              onCountChange={setInventoryValue}
              required
            />
            <Select 
              filterable={false}
              label="Inventory Tracking Type"
              options={[
                { value: "product", content: "Product" },
                { value: "variant", content: "Variant" },
                { value: "none", content: "None" },
              ]}
              value={trackingValue}
              onOptionChange={setTrackingValue}
              required
            />
          </FormGroup>
          {saveError && (
            <InlineError message="Failed to update the variant. Please try again." />
          )}
        </Form>
      )}
    </Modal>
  );
};

export function spawnVariantEditModal(
  id: number, 
  sku: string, 
  productId: number, 
  trackingType: string, 
  inventoryCount: number,
  context: string,
  onSuccess: () => void,
) {
  const props = { id, sku, productId, trackingType, inventoryCount, context, onSuccess };
  const modal = document.createElement("div");
  modal.id = "modal";
  const container = document.body.appendChild(modal);

  function close() {
    ReactDOM.unmountComponentAtNode(container);
    container.remove();
  }

  ReactDOM.render(<VariantEditModal closeModal={close} {...props} />, container);
}
