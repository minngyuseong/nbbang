const CACHE = "nbbang-shell-v2";

self.addEventListener("install", (event) => {
  event.waitUntil(caches.open(CACHE).then((cache) => cache.addAll(["/"])));
  self.skipWaiting();
});

self.addEventListener("activate", (event) => {
  event.waitUntil(caches.keys().then((keys) => Promise.all(keys.filter((key) => key !== CACHE).map((key) => caches.delete(key)))));
  self.clients.claim();
});

self.addEventListener("fetch", (event) => {
  if (event.request.mode === "navigate") event.respondWith(fetch(event.request).catch(() => caches.match("/")));
});

self.addEventListener("notificationclick", (event) => {
  event.notification.close();
  const url = event.notification.data?.url || "/";
  event.waitUntil(self.clients.matchAll({ type: "window", includeUncontrolled: true }).then((clients) => {
    const existing = clients[0];
    if (existing) { existing.navigate(url); return existing.focus(); }
    return self.clients.openWindow(url);
  }));
});

self.addEventListener("push", (event) => {
  let data = { title: "NBBANG", body: "새 정산 알림이 있습니다.", url: "/" };
  try { if (event.data) data = { ...data, ...event.data.json() }; } catch {}
  event.waitUntil(self.registration.showNotification(data.title, {
    body: data.body,
    data: { url: data.url },
    tag: `nbbang-${data.url}`,
  }));
});
