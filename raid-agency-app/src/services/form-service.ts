// src/services/form-service.ts
export const formConfigService = () => {
    return {
        getFormConfig: async () => {
            const url = `./raid-core-schema.json`; // Replace with actual URL or logic to get the form config URL
            const response = await fetch(url);

            if (!response.ok) {
                throw new Error(`Form config could not be fetched`);
            }
            const json = await response.json();
            const formConfig = json.$defs;
            return formConfig;
        }
    }
}

interface TransformResult<T> {
  data: T;
  metadata: Record<string, { tooltip?: string }>;
}

export const transformFormData = <T>(data: T, configData: JSONObject): TransformResult<T> => {
    // Transform the data as needed for the form submission
    // This is a placeholder function; implement your transformation logic here
    if (!data || !configData) {
        console.warn("No data or configData provided for transformation");
        return { data, metadata: {} };
    }
    const result = { ...data };
    const metadata: Record<string, { tooltip?: string }> = {};
    const convert = convertCase(configData, false);

     Object.keys(result).forEach(key => {
    const configMatch = convert[key];
    
    if (
      configMatch &&
      typeof configMatch === 'object' &&
      !Array.isArray(configMatch) &&
      'description' in configMatch &&
      typeof (configMatch as { description?: unknown }).description === 'string'
    ) {
      metadata[key] = {
        tooltip: (configMatch as { description: string }).description
      };
    }
  });
  return {
    data: result,
    metadata
  };
}
type JSONValue = string | number | boolean | null | JSONObject | JSONArray
type JSONObject = { [key: string]: JSONValue }
type JSONArray = JSONValue[]

function convertCase(obj: JSONObject, toUpper: boolean): JSONObject {
  return Object.fromEntries(
    Object.entries(obj).map(([key, value]) => [
      toUpper ? key.toUpperCase() : key.charAt(0).toLowerCase() + key.slice(1),
      typeof value === 'object' && value !== null
        ? Array.isArray(value)
          ? value.map((item) =>
              typeof item === 'object' && item !== null
                ? convertCase(item as JSONObject, toUpper)
                : item
            )
          : convertCase(value as JSONObject, toUpper)
        : value,
    ])
  )
}
