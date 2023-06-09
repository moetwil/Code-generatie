<script setup lang="ts">
import Navbar from './components/Navbar.vue';
import Footer from './components/Footer.vue';
import { useCurrentUserStore } from './stores/CurrentUserStore';
import { onMounted } from 'vue';
import { useAuthenticationStore } from './stores/AuthenticationStore';

const authenticationStore = useAuthenticationStore();
const currentUserStore = useCurrentUserStore();
onMounted(async () => {
    // fetch the user on app load and set it in cache
    await currentUserStore.fetchUser(authenticationStore.getTokenAndUserId());
});
</script>

<template>
  <Navbar />
  <RouterView />
  <Footer />
</template>

<style scoped></style>
