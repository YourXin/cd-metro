// Chengdu Metro PWA Service Worker
const CACHE_NAME = 'cdmetro-pwa-v1';

self.addEventListener('install', (event) => {
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(self.clients.claim());
});

// Network-first strategy for live transit data
self.addEventListener('fetch', (event) => {
  // Always fetch network for API routes
  if (event.request.url.includes('/api/')) {
    event.respondWith(fetch(event.request));
    return;
  }
  
  event.respondWith(
    fetch(event.request).catch(() => caches.match(event.request))
  );
});
