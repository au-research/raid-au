import { TreeViewBaseItem } from '@mui/x-tree-view/models'

export const findItem = (items: TreeViewBaseItem[], id: string): TreeViewBaseItem | null => {
    for (const item of items) {
        if (item.id === id) {
            return item
        }
        if (item.children) {
            const found = findItem(item.children, id)
            if (found) {
                return found;
            }
        }
    }
    return null
}