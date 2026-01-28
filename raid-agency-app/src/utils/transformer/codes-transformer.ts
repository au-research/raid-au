/* -------------------------------------------------------------------------- */
/*                               Type Definitions                             */
/* -------------------------------------------------------------------------- */

export interface CodeNode {
  id: string;
  name: string;
  label: string;
  url: string;
  selected: boolean;
  children: CodeNode[];
}

export interface FlatANZSRCCodes {
  code?: string;
  id?: string;
  name?: string;
  title?: string;
}

export interface NestedANZSRCCodes {
  code: string;
  name: string;
  groups?: {
    code: string;
    name: string;
    objectives?: {
      code: string;
      name: string;
    }[];
    fields?: {
      code: string;
      name: string;
    }[];
  }[];
}

export interface SDGCode {
  code?: string;
  id?: string;
  goal?: string | number;
  title?: string;
  name?: string;
  description?: string;
}

export interface TransformedData {
  "ANZSRC FOR": CodeNode[];
  "ANZSRC SEO": CodeNode[];
  //SDGs: CodeNode[];
}

/* -------------------------------------------------------------------------- */
/*                             URL Generation Helper                           */
/* -------------------------------------------------------------------------- */

function generateANZSRCUrl(id: string, type: "FOR" | "SEO"): string {
  const codeType = type === "FOR" ? "anzsrc-for" : "anzsrc-seo";
  return `https://linked.data.gov.au/def/${codeType}/2020/${id}`;
}

function generateSDGUrl(id: string): string {
  // SDGs might have a different URL pattern, adjust as needed
  return `https://sdgs.un.org/goals/goal${id.replace('.', '#target-')}`;
}

/* -------------------------------------------------------------------------- */
/*                     Transformation: ANZSRC (FOR / SEO)                     */
/* -------------------------------------------------------------------------- */

export function transformANZSRCCodes(
  codes: any[],
  type: "FOR" | "SEO"
): CodeNode[] {
  if (codes.length > 0 && codes[0].groups) {
    return transformNestedANZSRCCodes(codes as NestedANZSRCCodes[], type);
  }
  return transformFlatANZSRCCodes(codes as FlatANZSRCCodes[], type);
}

export function transformFlatANZSRCCodes(
  codes: FlatANZSRCCodes[],
  type: "FOR" | "SEO"
): CodeNode[] {
  const codeMap = new Map<string, CodeNode>();
  const rootNodes: CodeNode[] = [];

  codes.forEach((code) => {
    const id = String(code.code || code.id);
    const name = code.name || code.title || "";
    const node: CodeNode = {
      id,
      name,
      label: `${id} - ${name}`,
      url: generateANZSRCUrl(id, type),
      selected: false,
      children: [],
    };
    codeMap.set(id, node);
  });

  codeMap.forEach((node, codeId) => {
    const len = codeId.length;

    if (len === 2) {
      rootNodes.push(node);
    } else if (len === 4) {
      const parent = codeMap.get(codeId.substring(0, 2));
      parent ? parent.children.push(node) : rootNodes.push(node);
    } else if (len === 6) {
      const parent = codeMap.get(codeId.substring(0, 4));
      if (parent) parent.children.push(node);
      else {
        const grandParent = codeMap.get(codeId.substring(0, 2));
        grandParent ? grandParent.children.push(node) : rootNodes.push(node);
      }
    } else {
      rootNodes.push(node);
    }
  });

  sortHierarchy(rootNodes);
  return rootNodes;
}

export function transformNestedANZSRCCodes(
  codes: NestedANZSRCCodes[],
  type: "FOR" | "SEO"
): CodeNode[] {
  const rootNodes: CodeNode[] = [];

  codes.forEach((category) => {
    const rootNode: CodeNode = {
      id: category.code,
      name: category.name,
      label: `${category.code} - ${category.name}`,
      url: generateANZSRCUrl(category.code, type),
      selected: false,
      children: [],
    };

    category.groups?.forEach((group) => {
      const groupNode: CodeNode = {
        id: group.code,
        name: group.name,
        label: `${group.code} - ${group.name}`,
        url: generateANZSRCUrl(group.code, type),
        selected: false,
        children: [],
      };

      group.objectives?.forEach((objective) => {
        groupNode.children.push({
          id: objective.code,
          name: objective.name,
          label: `${objective.code} - ${objective.name}`,
          url: generateANZSRCUrl(objective.code, type),
          selected: false,
          children: [],
        });
      });
      
      group.fields?.forEach((field) => {
        groupNode.children.push({
          id: field.code,
          name: field.name,
          label: `${field.code} - ${field.name}`,
          url: generateANZSRCUrl(field.code, type),
          selected: false,
          children: [],
        });
      });

      rootNode.children.push(groupNode);
    });

    rootNodes.push(rootNode);
  });

  sortHierarchy(rootNodes);
  return rootNodes;
}

/* -------------------------------------------------------------------------- */
/*                               SDG Transform                                 */
/* -------------------------------------------------------------------------- */

export function transformSDGs(codes: SDGCode[]): CodeNode[] {
  const codeMap = new Map<string, CodeNode>();
  const rootNodes: CodeNode[] = [];

  codes.forEach((code) => {
    const id = String(code.code || code.id || code.goal);
    const name = code.name || code.title || code.description || "";
    codeMap.set(id, {
      id,
      name,
      label: `${id} - ${name}`,
      url: generateSDGUrl(id),
      selected: false,
      children: [],
    });
  });

  codeMap.forEach((node, codeId) => {
    if (codeId.includes(".")) {
      const parentId = codeId.split(".")[0];
      const parent = codeMap.get(parentId);
      parent ? parent.children.push(node) : rootNodes.push(node);
    } else {
      rootNodes.push(node);
    }
  });

  sortHierarchy(rootNodes);
  return rootNodes;
}

/* -------------------------------------------------------------------------- */
/*                                Utilities                                    */
/* -------------------------------------------------------------------------- */

export function sortHierarchy(nodes: CodeNode[]): void {
  nodes.sort((a, b) => {
    const aId = a.id;
    const bId = b.id;

    if (/^\d+$/.test(aId) && /^\d+$/.test(bId)) {
      return Number(aId) - Number(bId);
    }

    if (aId.includes(".") || bId.includes(".")) {
      const aParts = aId.split(".").map(Number);
      const bParts = bId.split(".").map(Number);
      return aParts[0] - bParts[0] || (aParts[1] || 0) - (bParts[1] || 0);
    }

    return aId.localeCompare(bId, undefined, { numeric: true });
  });

  nodes.forEach((node) => {
    if (node.children?.length) sortHierarchy(node.children);
  });
}

export function flattenHierarchy(nodes: CodeNode[], acc: any[] = []): any[] {
  nodes.forEach((node) => {
    acc.push({
      id: node.id,
      name: node.name,
      label: node.label,
      url: node.url,
      selected: node.selected,
    });
    if (node.children?.length) flattenHierarchy(node.children, acc);
  });
  return acc;
}

export function findNodeById(
  nodes: CodeNode[],
  targetId: string
): CodeNode | null {
  for (const node of nodes) {
    if (node.id === targetId) return node;
    if (node.children?.length) {
      const found = findNodeById(node.children, targetId);
      if (found) return found;
    }
  }
  return null;
}

export function toggleNodeSelection(
  nodes: CodeNode[],
  targetId: string,
  selected: boolean
): void {
  const node = findNodeById(nodes, targetId);
  if (node) {
    node.selected = selected;
    node.children?.forEach((child) =>
      toggleNodeSelection(nodes, child.id, selected)
    );
  }
}

export function getStatistics(transformedData: TransformedData) {
  const stats = {
    "ANZSRC FOR": { totalNodes: 0, level1: 0, level2: 0, level3: 0 },
    "ANZSRC SEO": { totalNodes: 0, level1: 0, level2: 0, level3: 0 },
    "SDGs": { totalGoals: 0, totalTargets: 0 },
  };

  const forFlat = flattenHierarchy(transformedData["ANZSRC FOR"]);
  stats["ANZSRC FOR"].totalNodes = forFlat.length;
  forFlat.forEach((n) => {
    if (n.id.length === 2) stats["ANZSRC FOR"].level1++;
    else if (n.id.length === 4) stats["ANZSRC FOR"].level2++;
    else if (n.id.length === 6) stats["ANZSRC FOR"].level3++;
  });

  const seoFlat = flattenHierarchy(transformedData["ANZSRC SEO"]);
  stats["ANZSRC SEO"].totalNodes = seoFlat.length;
  seoFlat.forEach((n) => {
    if (n.id.length === 2) stats["ANZSRC SEO"].level1++;
    else if (n.id.length === 4) stats["ANZSRC SEO"].level2++;
    else if (n.id.length === 6) stats["ANZSRC SEO"].level3++;
  });

  /* const sdgFlat = flattenHierarchy(transformedData["SDGs"]);
  sdgFlat.forEach((n) => {
    if (n.id.includes(".")) stats["SDGs"].totalTargets++;
    else stats["SDGs"].totalGoals++;
  }); */

  return stats;
}
