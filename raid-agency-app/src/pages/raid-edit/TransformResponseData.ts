

export const addMissingEndDate = (jsonObj: unknown): unknown => {
    /**
     * Recursively traverses a JSON object and adds empty endDate ("") 
     * to any object that has startDate but no endDate
     * 
     * @param {Object|Array} jsonObj - The JSON object/array to process
     * @returns {Object|Array} - The modified JSON with missing endDates added
     */
    
    // Handle null or undefined
    if (jsonObj === null || jsonObj === undefined) {
        return jsonObj;
    }
    
    // Handle arrays
    if (Array.isArray(jsonObj)) {
        return jsonObj.map(item => addMissingEndDate(item));
    }
    
    // Handle objects
    if (typeof jsonObj === 'object') {
        const result: Record<string, unknown> = {};
        
        // Copy all properties and recursively process nested objects/arrays
        for (const key in jsonObj) {
            if (Object.prototype.hasOwnProperty.call(jsonObj, key)) {
                result[key] = addMissingEndDate((jsonObj as Record<string, unknown>)[key]);
            }
        }
        
        // Check if this object has startDate but no endDate
        if (Object.prototype.hasOwnProperty.call(result, 'startDate') && !Object.prototype.hasOwnProperty.call(result, 'endDate')) {
            result.endDate = "";
        }
        
        return result;
    }
    
    // Return primitive values as-is
    return jsonObj;
}

// Alternative function that modifies the original object in-place
interface JsonObject {
    [key: string]: JsonValue;
}

type JsonValue = string | number | boolean | null | JsonObject | JsonArray;
interface JsonArray extends Array<JsonValue> {}

export function addMissingEndDateInPlace(jsonObj: unknown): unknown {
    /**
     * Modifies the original JSON object in-place, adding empty endDate ("") 
     * to any object that has startDate but no endDate
     * 
     * @param {Object|Array} jsonObj - The JSON object/array to modify
     * @returns {Object|Array} - Reference to the modified original object
     */
    
    // Handle null or undefined
    if (jsonObj === null || jsonObj === undefined) {
        return jsonObj;
    }
    
    // Handle arrays
    if (Array.isArray(jsonObj)) {
        jsonObj.forEach(item => addMissingEndDateInPlace(item));
        return jsonObj;
    }
    
    // Handle objects
    if (typeof jsonObj === 'object') {
        // Recursively process nested objects/arrays
        for (const key in jsonObj as Record<string, unknown>) {
            if (Object.prototype.hasOwnProperty.call(jsonObj, key)) {
                const value = (jsonObj as Record<string, unknown>)[key];
                if (typeof value === 'object' && value !== null) {
                    addMissingEndDateInPlace(value);
                }
            }
        }
        
        // Check if this object has startDate but no endDate
        if (Object.prototype.hasOwnProperty.call(jsonObj, 'startDate') && !Object.prototype.hasOwnProperty.call(jsonObj, 'endDate')) {
            (jsonObj as Record<string, unknown>).endDate = "";
        }
    }
    
    return jsonObj;
}