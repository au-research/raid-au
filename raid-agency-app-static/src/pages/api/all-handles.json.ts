import fs from "node:fs";
import path from "node:path";

export function GET() {
  // Path to your existing JSON file in the src directory
  const jsonPath = path.resolve("./src/raw-data/all-handles.json");
  // check if the file exists and return empty array if not
  if (!fs.existsSync(jsonPath)) {
    return new Response("[]", {
      headers: {
        "Content-Type": "application/json",
      },
    });
  }

  const rawData = fs.readFileSync(jsonPath, "utf-8");

  return new Response(rawData, {
    headers: {
      "Content-Type": "application/json",
    },
  });
}
