import * as React from 'react';
import { RichTreeView, TreeItemProps } from '@mui/x-tree-view';
import AddBoxOutlinedIcon from '@mui/icons-material/AddBoxOutlined';
import IndeterminateCheckBoxOutlinedIcon from '@mui/icons-material/IndeterminateCheckBoxOutlined';
import { BorderedTreeItem } from './TreeViewStyles';
import { TreeViewBaseItem } from '@mui/x-tree-view/models'
import { findItem } from './CustomTree';

export default function CustomizedTreeViewWithSelection() {
    const [selectedIds, setSelectedIds] = React.useState<string[]>([]);
    const [treeItems, setTreeItems] = React.useState<TreeViewBaseItem[]>([]);

    React.useEffect(() => {
        fetch('/SubjectSample.JSON')
            .then(res => res.json())
            .then(data => {setTreeItems(data["ANZSRC FOR"])});
    }, []);

    const handleSelectedItemsChange = (event: React.SyntheticEvent | null, newSelectedIds: string[]) => {
        // event is unused; update controlled selection state with the new item IDs
        setSelectedIds(newSelectedIds);
    };

    function CustomTreeItem(props: TreeItemProps) {
        const item = findItem(treeItems, props.itemId);
        const hasChildren = item && item.children && item.id.length === 2;
        const isLastChild = item && !item.children;
        return (
            <BorderedTreeItem
                {...props}
                // Pass a custom data attribute to allow for prop-based conditional styling in StyledTreeItem
                data-iscustomparent={hasChildren ? 'true' : 'false'}
                data-isLastChild={isLastChild ? 'true' : 'false'}
                // Override the endIcon slot to render nothing, removing the icon from leaf nodes.
                slots={{ endIcon: () => <></> }}
            />
        )
    }

    return (
        <RichTreeView
            aria-label="customized tree view with selection"
            items={treeItems}
            // --- Make selection controlled to retrieve values ---
            checkboxSelection
            multiSelect
            selectedItems={selectedIds}
            onSelectedItemsChange={handleSelectedItemsChange}
            // --- Enable selection propagation for full feature implementation ---
            selectionPropagation={{
                parents: false, // Select parent if all children are selected
                descendants: false, // Select all children if parent is selected
            }}
            slots={{
                item: CustomTreeItem,
                expandIcon: AddBoxOutlinedIcon,
                collapseIcon: IndeterminateCheckBoxOutlinedIcon,
            }}
            sx={{ overflowX: 'hidden', minHeight: 270, flexGrow: 1, maxWidth: 'auto'}}
        />
    );
}
