<template>
    <div class="container py-5 d-flex justify-content-center flex-column">
        <div class="row pb-3">
            <!-- FullName of the user -->
            <div class="col d-flex justify-content-center align-items-center py-2">
                <h1>User: {{ user.firstName }} {{ user.lastName }} {{ userId }}</h1>
            </div>

        </div>
        <!-- Select atm option -->
        <div class="row">
            <!-- Col met alle userdata -->
            <div class="col-md-6 bg-light rounded p-3 m-2">
                <h2>Gegevens</h2>
                <div>
                    <label for="email">Email:</label>
                    <input v-model="user.email" id="email" placeholder="Email" />
                </div>
                <div>
                    <label for="firstName">Voornaam:</label>
                    <input v-model="user.firstName" id="firstName" placeholder="First Name" />
                </div>
                <div>
                    <label for="lastName">Achternaam:</label>
                    <input v-model="user.lastName" id="lastName" placeholder="Last Name" />
                </div>
                <div>
                    <label for="phoneNumber">Telefoonnummer:</label>
                    <input v-model="user.phoneNumber" id="phoneNumber" placeholder="Phone Number" />
                </div>
                <div>
                    <label for="dayLimit">Dag limiet:</label>
                    <input v-model="user.dayLimit" id="dayLimit" placeholder="Day Limit" />
                </div>
                <div>
                    <label for="transactionLimit">Transactie limiet:</label>
                    <input v-model="user.transactionLimit" id="transactionLimit" placeholder="Transaction Limit" />
                </div>
                <button @click="saveUser" class="btn-primary">Opslaan</button>
                <p id="message">{{ message }}</p>
            </div>
            <div class="col ">
                <div class="row bg-light rounded p-3 m-2">
                    <h2>Acties</h2>
                    <div class="row">
                        <div class="col">
                            <button class="btn-primary" :disabled="user.accounts?.length == 2">Rekening toevoegen</button>
                        </div>
                        <div class="col">
                            <button class="btn-secondary" @click="handleBlockState"> {{ isBlocked ? 'Deblokkeer gebruiker' :
                                'Blokkeer gebruiker' }}
                            </button>
                        </div>
                    </div>
                    <div class="row mt-3">
                        <div class="col">
                            <button class="btn-secondary" :disabled="user.accounts?.length != 0">Gebruiker
                                verwijderen</button>
                        </div>
                    </div>
                </div>
                <div class="row bg-light rounded m-2 mt-3 p-3">
                    <h2>Rekeningen</h2>
                    <AccountPreviewDashboard v-for="account in user.accounts?.sort((a, b) => a.accountType - b.accountType)"
                        :key="account.id" :iban="account.iban" :balance="account.balance"
                        :accountType="account.accountType === 0 ? 'Betaal' : 'Spaar'" class="account rounded"
                        @click="handleClickOnAccount(account.id)" />
                </div>

            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { useUserStore } from '../stores/UserStore';
import { useAuthenticationStore } from '../stores/AuthenticationStore';
import { useAccountStore } from '../stores/AccountStore';
import { ref, onMounted, reactive, computed } from 'vue';
import User from '../interfaces/User';
import router from '../router';
import AccountPreviewDashboard from '../components/AccountPreviewDashboard.vue';
import { RoleType } from '../enums/RoleType';

const userStore = useUserStore();
const authenticationStore = useAuthenticationStore();
const accountStore = useAccountStore();

onMounted(async () => {
    // Check if the user is authenticated
    if (!authenticationStore.isLoggedIn) {
        router.push('/');
    }

    // get checkUserId from localstorage adn make number
    if (!localStorage.getItem('checkUserId')) {
        router.push('/dashboard');
    }
    const checkUserId: number = Number(localStorage.getItem('checkUserId'));

    await userStore.fetchUser(checkUserId);

    await userStore.fetchAccounts();
    Object.assign(user, userStore.getUser);

});

const user = reactive<User>({
    id: 0,
    firstName: '',
    bsn: '',
    lastName: '',
    email: '',
    phoneNumber: '',
    role: RoleType.CUSTOMER,
    accounts: [],
    dayLimit: 0,
    transactionLimit: 0,
    blocked: false
});

const userId = userStore.getUserId;
const message = ref('');

const isBlocked = computed(() => {
    return user.blocked ?? false;
});

async function saveUser() {
    try {
        // Call a method in the userStore to save the user data
        await userStore.updateUser(user);
        Object.assign(user, userStore.getUser);

        message.value = 'User data saved successfully.';
    } catch (error) {
        message.value = 'Error occurred while saving user data.';
    }
}


async function handleBlockState() {
    try {
        // if the user is not blocked yet
        if (!isBlocked.value) {
            await userStore.blockUser();
        } else {
            await userStore.unblockUser();
        }

        Object.assign(user, userStore.getUser);
    } catch (error) {
        message.value = 'Error occurred while saving user data.';
    }
}

const handleClickOnAccount = async (id: any) => {
    // Use the `iban` parameter as needed
    await accountStore.fetchAccount(id);
    router.push('/account');
};
</script>

<style scoped>
.account {
    background-color: #F2F3F6 !important;
    margin: 1px;
    width: 100% !important;
    cursor: pointer;
}

/* button disabled */
.btn-primary:disabled {
    background-color: #6c757d !important;
    border-color: #6c757d !important;
}
</style>