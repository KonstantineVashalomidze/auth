import { requestOtp, verifyOtp } from './auth.api.js';

/* UI */
function showStep(stepId) {
    document.querySelectorAll('.step').forEach(el => el.hidden = true);
    document.getElementById(stepId).hidden = false;
}

function setError(elementId, message) {
    document.getElementById(elementId).textContent = message ?? 'Something went wrong.';
}

function clearError(elementId) {
    document.getElementById(elementId).textContent = '';
}

function startOtpCountdown(seconds) {
    const el = document.querySelector('.step:not([hidden]) .otp-countdown');
    let remaining = seconds;

    const interval = setInterval(() => {
        remaining -= 1;
        el.textContent = `Code expires in ${remaining}s`;

        if (remaining <= 0) {
            clearInterval(interval);
            el.textContent = 'Code expired. Please go back and request a new one.';
        }
    }, 1_000);
}

function setServerMessage(message) {
    document.querySelector('.step:not([hidden]) .server-message').textContent = message;
}


/* HANDLERS */

async function handleEmailSubmit(e) {
    e.preventDefault();
    clearError('email-error');

    const email = e.target.email.value;

    try {
        const res = await requestOtp(email);
        sessionStorage.setItem('pendingEmail', email);

        showStep(res.accountStatus === 'NEW' ? 'register-step' : 'login-step');
        setServerMessage(res.message);
        startOtpCountdown(res.otpExpiresInSeconds);
    } catch (err) {
        setError('email-error', err.message);
    }
}

async function handleLoginSubmit(e) {
    e.preventDefault();
    clearError('login-error');

    const email = sessionStorage.getItem('pendingEmail');
    const otp   = e.target.otp.value;

    try {
        const res = await verifyOtp({ email, otp });
        sessionStorage.setItem('userId', res.userId);
        window.location.href = '/dashboard/index.html';
    } catch (err) {
        setError('login-error', err.message);
    }
}

async function handleRegisterSubmit(e) {
    e.preventDefault();
    clearError('register-error');

    const email         = sessionStorage.getItem('pendingEmail');
    const otp           = e.target.otp.value;
    const displayName   = e.target['display-name'].value;
    const age           = Number(e.target.age.value);
    const termsAccepted = e.target['terms-and-conditions'].checked;

    try {
        const res = await verifyOtp({ email, otp, displayName, age, termsAccepted });
        sessionStorage.setItem('userId', res.userId);
        window.location.href = '/dashboard/index.html';
    } catch (err) {
        setError('register-error', err.message);
    }
}

/* INIT */

document.getElementById('email-form').addEventListener('submit', handleEmailSubmit);
document.getElementById('login-form').addEventListener('submit', handleLoginSubmit);
document.getElementById('register-form').addEventListener('submit', handleRegisterSubmit);