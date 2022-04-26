import { useEffect, useState } from "react";
import { Box, Search } from "@bigcommerce/big-design";

interface SearchBarProps {
  defaultValue: string;
  onSearchSubmit: (value: string) => void;
}

export const SearchBar = ({ defaultValue, onSearchSubmit }: SearchBarProps) => {
  const [searchValue, setSearchValue] = useState("");

  useEffect(() => {
    setSearchValue(defaultValue);
  }, [defaultValue]);

  const onSearchChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setSearchValue(event.target.value);
  };

  const onSubmit = () => {
    onSearchSubmit(searchValue);
  };

  return (
    <Box marginBottom="medium">
      <Search value={searchValue} onChange={onSearchChange} onSubmit={onSubmit} />
    </Box>
  );
};
