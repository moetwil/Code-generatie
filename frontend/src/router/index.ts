import { RouteRecordRaw, createRouter, createWebHistory } from 'vue-router';
import Home from '../views/HomeView.vue';
import Atm from '../views/AtmView.vue';
import Account from '../views/AccountView.vue';
import Login from '../views/LoginView.vue';
import AllUsersView from '../views/AllUsersView.vue';
import AllAccountsView from '../views/AllAccountsView.vue';
import CustomerDashboard from '../views/DashboardView.vue';
import EmployeeDashboard from '../views/EmployeeDashboardView.vue';
import EditAccountView from '../views/EditAccountView.vue';
import User from '../views/UserView.vue';
import MyUserAccountView from '../views/MyUserAccountView.vue';
import TransferView from '../views/TransferView.vue';
import AllTransactionsView from "../views/AllTransactionsView.vue"
import { useCurrentUserStore } from '../stores/CurrentUserStore';

// route guards
const visitorRequired = () => {
  const CurrentUserStore = useCurrentUserStore();
  if (CurrentUserStore.isLoggedIn)
    return { path: "/dashboard" };
};
const customerRequired = () => {
  const CurrentUserStore = useCurrentUserStore();
  if (!CurrentUserStore.isLoggedIn)
    return { path: "/" };
};
const employeeRequired = () => {
  const CurrentUserStore = useCurrentUserStore();
  if (CurrentUserStore.isLoggedIn)
    if (!CurrentUserStore.getIsEmployee)
      return { path: "/dashboard" };
    else
      return { path: "/" };
};

// Define routes
const routes = [
  { path: '/', component: Login, beforeEnter: visitorRequired },
  { path: '/atm', component: Atm, beforeEnter: customerRequired, },
  { path: '/login', component: Login, beforeEnter: visitorRequired },
  { path: '/dashboard', component: CustomerDashboard, beforeEnter: customerRequired },
  { path: '/employee', component: EmployeeDashboard, beforeEnter: employeeRequired },
  { path: '/account', component: Account, beforeEnter: customerRequired },
  { path: '/transfer', component: TransferView, beforeEnter: customerRequired },
  { path: '/employee/transactions', component: AllTransactionsView, beforeEnter: employeeRequired },
  { path: '/employee/transfer',
    component: TransferView,
    beforeEnter: employeeRequired,
    props: { employeeView: true },
  },
  { path: '/users', component: AllUsersView, beforeEnter: employeeRequired },
  { path: '/accounts', component: AllAccountsView, beforeEnter: employeeRequired },
  { path: '/edit-account', component: EditAccountView, beforeEnter: employeeRequired },
  { path: '/user', component: User },
  { path: '/mijn-account', component: MyUserAccountView, beforeEnter: customerRequired },
] satisfies RouteRecordRaw[];

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
});

export default router;
