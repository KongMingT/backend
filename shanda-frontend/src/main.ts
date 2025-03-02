import { createApp } from "vue";
import { createPinia } from "pinia";
import App from "./App.vue";
import ArcoVue from "@arco-design/web-vue";
import "@arco-design/web-vue/dist/arco.css";
import router from "./router";
import "@/assets";

const pinia = createPinia();
createApp(App).use(pinia).use(router).use(ArcoVue).mount("#app");
