// COOP/COEP hlavičky — bez nich prohlížeč nepovolí SharedArrayBuffer, který potřebuje SQLite
// ve WASM workeru.
//
// Guard na `config.devServer`: tenhle snippet se vkládá do KAŽDÉ webpack konfigurace, ale
// `devServer` existuje jen v dev buildu. V produkčním (jsBrowserProductionWebpack) je undefined
// a bez guardu celý build spadne na "Cannot set properties of undefined (setting 'headers')".
;(function(config) {
    if (!config.devServer) return;

    config.devServer.headers = [
        { key: 'Cross-Origin-Opener-Policy', value: 'same-origin' },
        { key: 'Cross-Origin-Embedder-Policy', value: 'require-corp' }
    ]
})(config);
