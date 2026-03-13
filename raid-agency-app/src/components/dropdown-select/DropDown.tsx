import * as React from 'react';
import Box from '@mui/material/Box';
import InputLabel from '@mui/material/InputLabel';
import MenuItem from '@mui/material/MenuItem';
import FormControl from '@mui/material/FormControl';
import Select, { SelectChangeEvent } from '@mui/material/Select';

export default function DropDown(props: {
    label: string,
    options: string[],
    defaultValue?: string,
    currentValue?: string,
    setValue:
    (value: string) => void,
}) {
  const [value, setValue] = React.useState(props.currentValue || props.defaultValue);
  const handleChange = (event: SelectChangeEvent) => {
    const newValue = event.target.value as string;
    setValue(newValue);
    if (props.setValue) {
      props.setValue(newValue);
    }
  };

  // Determine the current value, ensuring it's valid
  const currentValue = props.currentValue || value || '';
  const isValidValue = props.options.includes(currentValue);

  return (
    <Box sx={{ width: { xs: '100%', sm: 200, md: 250, lg: 300, xl: 400 } }}>
      <FormControl fullWidth>
        <InputLabel id="demo-simple-select-label">{props.label}</InputLabel>
        <Select
          labelId="demo-simple-select-label"
          id="demo-simple-select"
          value={isValidValue ? currentValue : ''}
          label={props.label}
          onChange={handleChange}
        >
          {props.options.map((option) => (
            <MenuItem key={option} value={option}>
              {option}
            </MenuItem>
          ))}
        </Select>
      </FormControl>
    </Box>
  );
}
