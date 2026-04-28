(function () {
    console.log("--- Penalty Bridge: HIJACK MODE ---");

    function notifyReady() {
        if (typeof Android !== "undefined" && Android.onRuntimeReady) {
            Android.onRuntimeReady();
        } else {
            setTimeout(notifyReady, 1000);
        }
    }
    notifyReady();

    var bridgeStartTime   = Date.now();
    var gameOverFired     = false;
    var isGameActive      = false;
    var lastScore         = 0;
    var clickInterval     = null;
    var goToLayoutHooked  = false;
    var hookAttempted     = false;

    // ── Global Hooks ────────────────────────────────────────────────
    window.onGameOver = function(score) { triggerGameOver("Global onGameOver", score); };
    if (typeof Android !== "undefined" && typeof MainJavaClass === "undefined") {
        window.MainJavaClass = Android;
    }

    window._androidStartGame = function () {
        bridgeStartTime      = Date.now();
        gameOverFired        = false;
        isGameActive         = false;
        lastScore            = 0;
        
        if (typeof Android !== "undefined" && typeof MainJavaClass === "undefined") {
            window.MainJavaClass = Android;
        }

        if (clickInterval) { clearInterval(clickInterval); clickInterval = null; }

        var attempts = 0;
        clickInterval = setInterval(function () {
            attempts++;
            var canvas = document.querySelector('canvas');
            if (canvas) {
                var r = canvas.getBoundingClientRect();
                var opts = { bubbles: true, cancelable: true,
                             clientX: r.width * 0.5, clientY: r.height * 0.51 };
                canvas.dispatchEvent(new PointerEvent('pointerdown', opts));
                canvas.dispatchEvent(new MouseEvent ('mousedown',    opts));
                setTimeout(function () {
                    canvas.dispatchEvent(new PointerEvent('pointerup', opts));
                    canvas.dispatchEvent(new MouseEvent ('mouseup',    opts));
                    canvas.dispatchEvent(new MouseEvent ('click',      opts));
                }, 500); // 500ms delay to allow game to setup
            }
            if (attempts >= 8) { clearInterval(clickInterval); clickInterval = null; }
        }, 300);
    };

    window._androidSetSound = function (enabled) { };

    function hookGoToLayout(rt) {
        if (goToLayoutHooked || hookAttempted) return;
        hookAttempted = true;

        var methodNames = ['GoToLayout', '_GoToLayout', 'goToLayout', 'Gv', 'hv'];
        var proto = Object.getPrototypeOf(rt);

        var target = null;
        var targetProto = null;
        var chain = [rt, proto];
        if (proto) chain.push(Object.getPrototypeOf(proto));

        for (var ci = 0; ci < chain.length; ci++) {
            var obj = chain[ci];
            for (var mi = 0; mi < methodNames.length; mi++) {
                if (obj && typeof obj[methodNames[mi]] === 'function') {
                    target = methodNames[mi];
                    targetProto = obj;
                    break;
                }
            }
            if (target) break;
        }

        if (target && targetProto) {
            var original = targetProto[target];
            targetProto[target] = function () {
                var args = arguments;
                var destLayout = args[0];
                var dest = (typeof destLayout === 'string') ? destLayout
                         : (destLayout && (destLayout.name || destLayout._name)) ? (destLayout.name || destLayout._name) : '';
                
                if (isGameActive && !gameOverFired) {
                    if (dest === 'MainMenu' || dest === 'Penalty' || dest === 'Splash' || dest === '') {
                        triggerGameOver("GoToLayout hook → '" + dest + "'");
                    }
                }
                return original.apply(this, args);
            };
            goToLayoutHooked = true;
            console.log("BRIDGE: Hooked '" + target + "'");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // HOME BUTTON HIJACKER (INVISIBLE OVERLAY)
    // ═══════════════════════════════════════════════════════════════
    var hijackOverlay = null;

    function applyHijackOverlay() {
        if (hijackOverlay) return;
        
        try {
            var canvas = document.querySelector('canvas');
            if (!canvas) return;

            var rect = canvas.getBoundingClientRect();
            
            // Create a transparent click area over the home button spot
            // The Home icon is at ~88% width, ~5% height
            hijackOverlay = document.createElement('div');
            hijackOverlay.style.cssText = 'position:fixed; z-index:99999; background:rgba(0,0,0,0);'
                                       + 'right:10%; top:2%; width:15%; height:10%; cursor:pointer;';
            
            hijackOverlay.onclick = function(e) {
                console.log("BRIDGE: Home Button Area Click Hijacked!");
                e.stopPropagation();
                e.preventDefault();
                if (typeof Android !== "undefined" && Android.onHomeClicked) {
                    Android.onHomeClicked();
                } else if (typeof MainJavaClass !== "undefined" && MainJavaClass.onHomeClicked) {
                    MainJavaClass.onHomeClicked();
                }
            };
            
            document.body.appendChild(hijackOverlay);
            console.log("BRIDGE: Hijack Overlay Attached ✓");
        } catch (e) {}
    }

    // ═══════════════════════════════════════════════════════════════
    // CORE POLLING
    // ═══════════════════════════════════════════════════════════════
    var bootLayoutName = "";
    var bootLayerCount = 0;

    function hookRuntime() {
        try {
            var rt = (window.c3_runtimeInterface && window.c3_runtimeInterface._GetLocalRuntime)
                   ? window.c3_runtimeInterface._GetLocalRuntime() : null;

            if (rt) {
                if (!goToLayoutHooked) hookGoToLayout(rt);
            }
            
            // Attach the hijack overlay once
            applyHijackOverlay();

            if (rt) {
                var cur = rt.GetMainLayout ? rt.GetMainLayout()
                        : rt._layout ? rt._layout
                        : rt._mainLayout ? rt._mainLayout : null;

                if (cur) {
                    var elapsed = Date.now() - bridgeStartTime;
                    var curName = (cur.name || cur._name || cur.Fs || "").toLowerCase();
                    var layerCount = 0;
                    try {
                        layerCount = cur.GetLayerCount ? cur.GetLayerCount()
                                   : cur._layers ? cur._layers.length
                                   : cur.fs ? cur.fs.length : 0;
                    } catch(e) {}

                    if (elapsed < 1500 && bootLayoutName === "") {
                        bootLayoutName = curName;
                        bootLayerCount = layerCount;
                    }

                    if (!isGameActive && elapsed > 1000) {
                        var layoutChanged = (bootLayoutName !== "" && curName !== "" && curName !== bootLayoutName);
                        var layersChanged = (bootLayerCount > 0 && layerCount > 0 && layerCount !== bootLayerCount);
                        
                        if (layoutChanged || layersChanged || curName === "game") {
                            isGameActive = true;
                            if (typeof Android !== "undefined") Android.onGameStarted();
                        }
                    }

                    if (isGameActive && !gameOverFired && elapsed > 2500) {
                        if (curName === bootLayoutName || (layerCount === bootLayerCount && bootLayerCount > 0 && curName !== "game")) {
                            triggerGameOver("Polling END (" + curName + ")");
                        }
                    }
                }
            }
        } catch (e) { }
        setTimeout(hookRuntime, 50);
    }

    function triggerGameOver(reason, score) {
        if (gameOverFired) return;
        gameOverFired = true;
        
        var finalScore = score !== undefined ? score : lastScore;
        console.log("BRIDGE END: " + reason + " Score: " + finalScore);

        try {
            var canvas = document.querySelector('canvas');
            if (canvas) canvas.style.display = 'none';
            document.body.style.background = '#000';
            document.body.style.visibility = 'hidden';
        } catch (e) { }

        setTimeout(function () {
            if (typeof Android !== "undefined" && Android.onGameOver) {
                Android.onGameOver(finalScore);
            }
            if (typeof MainJavaClass !== "undefined" && MainJavaClass.onGameOver) {
                 MainJavaClass.onGameOver(finalScore);
            }
        }, 30);
    }

    hookRuntime();
})();