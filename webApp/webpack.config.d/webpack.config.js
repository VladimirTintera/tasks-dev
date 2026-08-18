// COOP/COEP headers — without them the browser refuses SharedArrayBuffer, which SQLite in the
// WASM worker needs.
//
// The `config.devServer` guard matters: this snippet is injected into EVERY webpack config, but
// `devServer` only exists in the dev build. In the production one (jsBrowserProductionWebpack) it
// is undefined, and without the guard the whole build fails with
// "Cannot set properties of undefined (setting 'headers')".
;(function(config) {
    if (!config.devServer) return;

    config.devServer.headers = [
        { key: 'Cross-Origin-Opener-Policy', value: 'same-origin' },
        { key: 'Cross-Origin-Embedder-Policy', value: 'require-corp' }
    ]
})(config);
