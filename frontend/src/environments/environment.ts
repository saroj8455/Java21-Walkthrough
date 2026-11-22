/**
 * The API base URL is RELATIVE on purpose:
 *  - in Docker, nginx proxies /api -> backend:8080 (see frontend/nginx.conf)
 *  - with `npm start`, the dev server proxies /api -> localhost:8080 (see proxy.conf.json)
 * Browser always talks to its own origin => no CORS configuration needed on the backend.
 */
export const environment = {
  apiBaseUrl: '/api/v1',
};
