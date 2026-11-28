import { useMemo } from 'react'
import CrudPage from '../components/CrudPage.tsx'
import type { Column } from '../components/DataTable.tsx'
import type { FieldConfig } from '../components/EntityFormModal.tsx'
import { useCrud } from '../hooks/useCrud.ts'
import {
  conversionInputSchema,
  conversionSchema,
  type Conversion,
  type ConversionFormValues,
  type ConversionInput,
} from '../schemas/conversion.ts'
import { productSchema, type Product } from '../schemas/product.ts'
import { unitOfMeasureSchema, type UnitOfMeasure } from '../schemas/unitOfMeasure.ts'
import { markDeleted, withDeleted } from '../utils/deleted.ts'

const emptyValues: ConversionFormValues = {
  productId: '',
  parentHowSoldId: '',
  childHowSoldId: '',
  unitConversion: '',
}

const ALL_PRODUCTS = 'All products'

export default function UnitConversions() {
  const {
    items: units,
    loading: unitsLoading,
    error: unitsError,
  } = useCrud<UnitOfMeasure>('/api/how-sold', { schema: unitOfMeasureSchema })
  const { items: products } = useCrud<Product>('/api/products', { schema: productSchema })

  const productName = useMemo(() => new Map(products.map((p) => [p.id, p.name])), [products])

  const columns = useMemo<Column<Conversion>[]>(() => {
    const parent = (c: Conversion) => markDeleted(c.parentHowSoldName, c.parentHowSoldActive)
    const child = (c: Conversion) => markDeleted(c.childHowSoldName, c.childHowSoldActive)
    const factor = (c: Conversion) => c.unitConversion.toLocaleString(undefined, { maximumFractionDigits: 4 })
    const product = (c: Conversion) => (c.productId ? (productName.get(c.productId) ?? '—') : ALL_PRODUCTS)
    return [
      { key: 'product', header: 'Product', render: product, searchValue: product },
      {
        key: 'parent',
        header: 'Larger unit',
        render: parent,
        searchValue: (c) => c.parentHowSoldName,
      },
      {
        key: 'child',
        header: 'Smaller unit',
        render: child,
        searchValue: (c) => c.childHowSoldName,
      },
      { key: 'unitConversion', header: 'Conversion', render: factor, searchValue: (c) => c.unitConversion },
      {
        key: 'meaning',
        header: 'Meaning',
        render: (c) => `1 ${parent(c)} = ${factor(c)} ${child(c)}`,
        searchValue: (c) => `${c.parentHowSoldName} ${c.childHowSoldName}`,
      },
    ]
  }, [productName])

  const unitOptions = useMemo(() => units.map((u) => ({ value: u.id, label: u.name })), [units])

  const fields = (editing: Conversion | null): FieldConfig<ConversionFormValues>[] => [
    {
      name: 'productId',
      label: 'Product',
      type: 'select',
      placeholder: ALL_PRODUCTS,
      options: products.map((p) => ({ value: p.id, label: p.name })),
    },
    {
      name: 'parentHowSoldId',
      label: 'Larger unit (e.g. Case)',
      type: 'select',
      placeholder: 'Select a unit',
      options: withDeleted(
        unitOptions,
        ...(editing
          ? [{ id: editing.parentHowSoldId, name: editing.parentHowSoldName, active: editing.parentHowSoldActive }]
          : []),
      ),
    },
    {
      name: 'childHowSoldId',
      label: 'Smaller unit (e.g. Each)',
      type: 'select',
      placeholder: 'Select a unit',
      options: withDeleted(
        unitOptions,
        ...(editing
          ? [{ id: editing.childHowSoldId, name: editing.childHowSoldName, active: editing.childHowSoldActive }]
          : []),
      ),
    },
    { name: 'unitConversion', label: 'How many smaller units in 1 larger unit' },
  ]

  return (
    <CrudPage<Conversion, ConversionFormValues, ConversionInput>
      title="Unit Conversions"
      subtitle="How units relate for a product, for example 1 case = 12 each. Leave the product empty to apply to all products."
      singular="conversion"
      resource="/api/how-sold-conversions"
      schema={conversionSchema}
      columns={columns}
      fields={fields}
      inputSchema={conversionInputSchema}
      emptyValues={emptyValues}
      toFormValues={(c) => ({
        productId: c.productId ?? '',
        parentHowSoldId: c.parentHowSoldId,
        childHowSoldId: c.childHowSoldId,
        unitConversion: String(c.unitConversion),
      })}
      itemName={(c) => `1 ${c.parentHowSoldName} = ${c.unitConversion} ${c.childHowSoldName}`}
      permissionResource="units"
      searchPlaceholder="Search conversions…"
      notice={
        !unitsLoading && !unitsError && units.length < 2
          ? 'A conversion needs two units of measure. Add them on the Units of Measure page first.'
          : undefined
      }
    />
  )
}
