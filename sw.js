var CACHE = "leia-a-biblia-8";
var FILES = [
  "./",
  "./index.html",
  "./css/app.css",
  "./js/logic.js",
  "./js/app.js",
  "./manifest.webmanifest",
  "./assets/boneco.jpg",
  "./assets/boneco-feliz.jpg",
  "./assets/boneco-desconfiado.jpg",
  "./assets/tree-c02-botao.jpg",
  "./assets/tree-c03-flores.jpg",
  "./assets/tree-c04-rosa.jpg",
  "./assets/tree-c05-violeta.jpg",
  "./assets/tree-c06-frutos.jpg",
  "./assets/tree-c07-pomar.jpg",
  "./assets/tree-c08-vermelho.jpg",
  "./assets/tree-c09-ouro.jpg",
  "./assets/tree-c10-lanternas.jpg",
  "./assets/tree-c11-crepusculo.jpg",
  "./assets/tree-c12-iluminada.jpg",
  "./assets/tree-00-semente.jpg",
  "./assets/tree-01-broto.jpg",
  "./assets/tree-02-muda.jpg",
  "./assets/tree-03-planta.jpg",
  "./assets/tree-04-jovem.jpg",
  "./assets/tree-05-arvore.jpg",
  "./assets/tree-06-cheia.jpg",
  "./assets/tree-07-madura.jpg",
  "./assets/tree-08-grande.jpg",
  "./assets/tree-09-gigante.jpg",
  "./assets/tree-10-antiga.jpg"
];

self.addEventListener("install", function (event) {
  event.waitUntil(caches.open(CACHE).then(function (cache) { return cache.addAll(FILES); }));
  self.skipWaiting();
});

self.addEventListener("activate", function (event) {
  event.waitUntil(
    caches.keys().then(function (keys) {
      return Promise.all(keys.filter(function (key) { return key !== CACHE; }).map(function (key) {
        return caches.delete(key);
      }));
    }).then(function () { return self.clients.claim(); })
  );
});

self.addEventListener("fetch", function (event) {
  if (event.request.method !== "GET") return;
  event.respondWith(
    caches.match(event.request).then(function (cached) {
      if (cached) return cached;
      return fetch(event.request).then(function (response) {
        var copy = response.clone();
        caches.open(CACHE).then(function (cache) { cache.put(event.request, copy); });
        return response;
      }).catch(function () {
        if (event.request.mode === "navigate") return caches.match("./index.html");
        return new Response("", { status: 504 });
      });
    })
  );
});
