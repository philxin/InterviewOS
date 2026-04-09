import type { Component } from 'vue'
import {
  Collection,
  DataAnalysis,
  Document,
  FolderOpened,
  Management,
  Plus,
  Upload,
} from '@element-plus/icons-vue'

export interface AppNavigationItem {
  key: string
  label: string
  to: string
  icon: Component
  exactPaths: string[]
  prefixPaths?: string[]
}

export const appNavigationItems: AppNavigationItem[] = [
  {
    key: 'dashboard',
    label: '知识点',
    to: '/',
    icon: Management,
    exactPaths: ['/'],
    prefixPaths: ['/knowledge/edit/', '/training/', '/result/'],
  },
  {
    key: 'knowledge-import',
    label: '导入',
    to: '/knowledge/import',
    icon: Upload,
    exactPaths: ['/knowledge/import'],
  },
  {
    key: 'knowledge-file-import',
    label: '文件导入',
    to: '/knowledge/file-import',
    icon: Document,
    exactPaths: ['/knowledge/file-import'],
  },
  {
    key: 'knowledge-documents',
    label: '文档库',
    to: '/knowledge/documents',
    icon: FolderOpened,
    exactPaths: ['/knowledge/documents'],
  },
  {
    key: 'history',
    label: '历史',
    to: '/history',
    icon: DataAnalysis,
    exactPaths: ['/history'],
  },
  {
    key: 'invitations',
    label: '邀请',
    to: '/invitations',
    icon: Collection,
    exactPaths: ['/invitations'],
    prefixPaths: ['/invite/'],
  },
  {
    key: 'knowledge-create',
    label: '新建',
    to: '/knowledge/new',
    icon: Plus,
    exactPaths: ['/knowledge/new'],
  },
]

export function isNavigationActive(item: AppNavigationItem, path: string) {
  if (item.exactPaths.includes(path)) {
    return true
  }

  return item.prefixPaths?.some((prefix) => path.startsWith(prefix)) ?? false
}
