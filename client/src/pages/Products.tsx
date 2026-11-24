import { useMemo } from 'react'
import CrudPage from '../components/CrudPage.tsx'
import type { Column } from '../components/DataTable.tsx'
import type { FieldConfig } from '../components/EntityFormModal.tsx'
import { useCrud } from '../hooks/useCrud.ts'
import {
  categorySchema,
  productInputSchema,
  productSchema,
  type Category,
  type Product,
  type ProductFormValues,
  type ProductInput,
} from '../schemas/product.ts'

const emptyValues: ProductFormValues = { name: '', basePrice: '', categoryId: '' }

const formatPrice = (price: number) =>
  price.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })

export default function Products() {
  // Only used to name and offer categories, so a failure here just leaves the dropdown empty.
  const { items: categories } = useCrud<Category>('/api/categories', { schema: categorySchema })

  const categoryName = useMemo(() => new Map(categories.map((c) => [c.id, c.name])), [categories])

  const columns = useMemo<Column<Product>[]>(
    () => [
      { key: 'name', header: 'Name' },
      {
        key: 'category',
        header: 'Category',
        render: (p) => (p.categoryId ? (categoryName.get(p.categoryId) ?? '—') : '—'),
        searchValue: (p) => (p.categoryId ? categoryName.get(p.categoryId) : undefined),
      },
      {
        key: 'basePrice',
        header: 'Base price',
        render: (p) => formatPrice(p.basePrice),
        searchValue: (p) => p.basePrice,
      },
      {
        key: 'suppliers',
        header: 'Suppliers',
        render: (p) => p.supplierIds.length,
        searchValue: (p) => p.supplierIds.length,
      },
    ],
    [categoryName],
  )

  const fields = useMemo<FieldConfig<ProductFormValues>[]>(
    () => [
      { name: 'name', label: 'Name' },
      { name: 'basePrice', label: 'Base price' },
      {
        name: 'categoryId',
        label: 'Category',
        type: 'select',
        placeholder: 'No category',
        options: categories.map((c) => ({ value: c.id, label: c.name })),
      },
    ],
    [categories],
  )

  return (
    <CrudPage<Product, ProductFormValues, ProductInput>
      title="Products"
      subtitle="Manage your product catalog: names, categories and base prices."
      singular="product"
      resource="/api/products"
      schema={productSchema}
      columns={columns}
      fields={fields}
      inputSchema={productInputSchema}
      emptyValues={emptyValues}
      toFormValues={(p) => ({
        name: p.name,
        basePrice: p.basePrice.toFixed(2),
        categoryId: p.categoryId ?? '',
      })}
      itemName={(p) => p.name}
      permissionResource="products"
      searchPlaceholder="Search products…"
    />
  )
}
