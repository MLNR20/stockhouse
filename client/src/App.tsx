import { Route, Routes } from 'react-router-dom'
import Layout from './components/Layout.tsx'
import RequireAuth from './components/RequireAuth.tsx'
import RequirePage from './components/RequirePage.tsx'
import AccessPages from './pages/AccessPages.tsx'
import AccessRoles from './pages/AccessRoles.tsx'
import AccessUsers from './pages/AccessUsers.tsx'
import Aisles from './pages/Aisles.tsx'
import Analytics from './pages/Analytics.tsx'
import Categories from './pages/Categories.tsx'
import Containers from './pages/Containers.tsx'
import Customers from './pages/Customers.tsx'
import Employees from './pages/Employees.tsx'
import Forbidden from './pages/Forbidden.tsx'
import ForgotPassword from './pages/ForgotPassword.tsx'
import History from './pages/History.tsx'
import Home from './pages/Home.tsx'
import Inventory from './pages/Inventory.tsx'
import Login from './pages/Login.tsx'
import NotFound from './pages/NotFound.tsx'
import OrderDetail from './pages/OrderDetail.tsx'
import OrderNew from './pages/OrderNew.tsx'
import Orders from './pages/Orders.tsx'
import Products from './pages/Products.tsx'
import ReorderDetail from './pages/ReorderDetail.tsx'
import ReorderNew from './pages/ReorderNew.tsx'
import Reorders from './pages/Reorders.tsx'
import Reports from './pages/Reports.tsx'
import ResetPassword from './pages/ResetPassword.tsx'
import Search from './pages/Search.tsx'
import ShelfRows from './pages/ShelfRows.tsx'
import Shelves from './pages/Shelves.tsx'
import Shipments from './pages/Shipments.tsx'
import Suppliers from './pages/Suppliers.tsx'
import Trucks from './pages/Trucks.tsx'
import UnitConversions from './pages/UnitConversions.tsx'
import UnitsOfMeasure from './pages/UnitsOfMeasure.tsx'
import Warehouses from './pages/Warehouses.tsx'

/**
 * Every page below sits under RequirePage, which lets a user in only if their role has been assigned that page
 * (Access Control > Page Assignment). What they may do on the page (add, edit, delete) is decided by the role's
 * permissions.
 */
export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/forgot-password" element={<ForgotPassword />} />
      <Route path="/reset-password" element={<ResetPassword />} />
      <Route element={<RequireAuth />}>
        <Route element={<Layout />}>
          <Route element={<RequirePage />}>
            <Route path="/" element={<Home />} />
            <Route path="/inventory" element={<Inventory />} />
            <Route path="/units" element={<UnitsOfMeasure />} />
            <Route path="/conversions" element={<UnitConversions />} />
            <Route path="/orders" element={<Orders />} />
            <Route path="/orders/new" element={<OrderNew />} />
            <Route path="/orders/:orderId" element={<OrderDetail />} />
            <Route path="/reorders" element={<Reorders />} />
            <Route path="/reorders/new" element={<ReorderNew />} />
            <Route path="/reorders/:reorderId" element={<ReorderDetail />} />
            <Route path="/suppliers" element={<Suppliers />} />
            <Route path="/products" element={<Products />} />
            <Route path="/categories" element={<Categories />} />
            <Route path="/warehouses" element={<Warehouses />} />
            <Route path="/aisles" element={<Aisles />} />
            <Route path="/shelves" element={<Shelves />} />
            <Route path="/rows" element={<ShelfRows />} />
            <Route path="/containers" element={<Containers />} />
            <Route path="/customers" element={<Customers />} />
            <Route path="/analytics" element={<Analytics />} />
            <Route path="/reports" element={<Reports />} />
            <Route path="/search" element={<Search />} />
            <Route path="/trucks" element={<Trucks />} />
            <Route path="/shipments" element={<Shipments />} />
            <Route path="/employees" element={<Employees />} />
            <Route path="/history" element={<History />} />
            <Route path="/access/users" element={<AccessUsers />} />
            <Route path="/access/roles" element={<AccessRoles />} />
            <Route path="/access/pages" element={<AccessPages />} />
          </Route>
          <Route path="/forbidden" element={<Forbidden />} />
          <Route path="*" element={<NotFound />} />
        </Route>
      </Route>
    </Routes>
  )
}
