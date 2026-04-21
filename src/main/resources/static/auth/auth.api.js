import {createHttpClient} from "../shared/http-client.js";


const client = createHttpClient({ baseUrl: '/authentication' });
export const requestOtp = (email) => client.post('/otp/request', { email });
export const verifyOtp = (payload) => client.post('/otp/verify', payload);
export const logout = () => client.post('/logout', {});