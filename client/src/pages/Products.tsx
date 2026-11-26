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
import { formatMoney } from '../utils/format.ts'

const emptyValues: ProductFormValues = { name: '', basePrice: '', categoryId: '' }

export default function Products() {
  // Only used to name and offer categories, so a failure here just leaves the dropdown empty.
  // Deleted categories are included so a product filed under one still shows (and keeps) it.
  const { items: categories } = useCrud<Category>('/api/categories?includeInactive=true', { schema: categorySchema })

  const columns = useMemo<Column<Product>[]>(
    () => [
      { key: 'name', header: 'Name' },
      {
        key: 'category',
        header: 'Category',
        render: (p) =>
          p.categoryId ? (
            <span className={p.categoryActive ? '' : 'text-slate-400'}>
              {p.categoryName ?? '—'}
              {!p.categoryActive && <span className="ml-1.5 text-xs italic">(deleted)</span>}
            </span>
          ) : (
            '—'
          ),
        searchValue: (p) => p.categoryName,
      },
      {
        key: 'basePrice',
        header: 'Base price',
        render: (p) => formatMoney(p.basePrice),
        searchValue: (p) => p.basePrice,
      },
      {
        key: 'suppliers',
        header: 'Suppliers',
        render: (p) =>
          p.suppliers.length === 0 ? (
            '—'
          ) : (
            <ul className="space-y-0.5">
              {p.suppliers.map((s) => (
                <li key={s.id} className={s.active ? '' : 'text-slate-400'}>
                  {s.name}
                  {!s.active && <span className="ml-1.5 text-xs italic">(deleted)</span>}
                </li>
              ))}
            </ul>
          ),
        searchValue: (p) => p.suppliers.map((s) => s.name).join(' '),
      },
    ],
    [],
  )

  const fields = useMemo<FieldConfig<ProductFormValues>[]>(
    () => [
      { name: 'name', label: 'Name' },
      { name: 'basePrice', label: 'Base price (₱)' },
      {
        name: 'categoryId',
        label: 'Category',
        type: 'select',
        placeholder: 'No category',
        options: categories.map((c) => ({ value: c.id, label: c.active ? c.name : `${c.name} (deleted)` })),
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
