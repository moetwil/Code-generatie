import { RoleType } from './../enums/RoleType';
import AccountCompact from './AccountCompact';

interface User {
  id?: number;
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber: string;
  role: string;
  accounts?: AccountCompact[];
}

export default User;

