export function createHttpClient(defaultConfig = {}) {
    const { baseUrl = '', defaultHeaders = {} } = defaultConfig;

    async function request(endpoint, customOptions = {}, isRetry = false) {
        const url = `/api/v1${baseUrl}${endpoint}`;
        const headers = {
            'Content-Type': 'application/json',
            ...defaultHeaders,
            ...customOptions.headers,
        };

        const options = {
            ...customOptions,
            headers,
        };

        const response = await fetch(url, options);

        if (response.status === 401 && !isRetry) {
            const refreshResponse = await fetch('/api/v1/authentication/token/refresh', {
                method: 'POST',
            });

            if (!refreshResponse.ok) {
                window.location.href = '/auth/index.html';
                return;
            }

            return request(endpoint, customOptions, true);
        }

        // For responses with no body (204) return undefined
        const contentType = response.headers.get('Content-Type') || '';
        if (response.status === 204 || !contentType.includes('application/json')) {
            if (!response.ok) throw new Error(`Request failed with status ${response.status}`);
            return;
        }

        const data = await response.json();

        if (!response.ok) {
            throw data;
        }

        return data;
    }

    return {
        get: (endpoint, headers = {}) => {
            return request(endpoint, { method: 'GET', headers });
        },
        post: (endpoint, body, headers = {}) => {
            return request(endpoint, {
                method: 'POST',
                body: JSON.stringify(body),
                headers,
            });
        },
        put: (endpoint, body, headers = {}) => {
            return request(endpoint, {
                method: 'PUT',
                body: JSON.stringify(body),
                headers,
            });
        },
        patch: (endpoint, body, headers = {}) => {
            return request(endpoint, {
                method: 'PATCH',
                body: JSON.stringify(body),
                headers,
            });
        },
        delete: (endpoint, headers = {}) => {
            return request(endpoint, { method: 'DELETE', headers });
        },
    };
}