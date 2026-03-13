import fs from "node:fs";
import path from "node:path";

export function GET() {
  // Path to your existing JSON file in the src directory
  const jsonPath = path.resolve("./src/raw-data/raids.json");
  const rawData = fs.readFileSync(jsonPath, "utf-8");

  return new Response(rawData, {
    headers: {
      "Content-Type": "application/json",
    },
  });
}
