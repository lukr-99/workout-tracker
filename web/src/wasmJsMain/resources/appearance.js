// Sets the saved theme and accent on <html> before the first paint, so the loading screen and the
// page behind the canvas never flash the wrong colours. The app saves the choice under
// "ember.appearance" as "Mode|Accent" (see platform/BrowserAppearance.kt). A separate file, not an
// inline script, because the page's content security policy only allows scripts from itself.
(function () {
  var dark = window.matchMedia ? window.matchMedia('(prefers-color-scheme: dark)') : null;
  function apply() {
    var mode = 'System', accent = 'Ember';
    try {
      var parts = (localStorage.getItem('ember.appearance') || '').split('|');
      if (parts[0]) mode = parts[0];
      if (parts[1]) accent = parts[1];
    } catch (e) {}
    var isDark = mode === 'Dark' || (mode !== 'Light' && (!dark || dark.matches));
    var root = document.documentElement;
    root.setAttribute('data-theme', isDark ? 'dark' : 'light');
    root.setAttribute('data-accent', accent.toLowerCase());
    var meta = document.querySelector('meta[name="theme-color"]');
    if (meta) meta.setAttribute('content', isDark ? '#0F0D0C' : '#F7F4F0');
  }
  window.emberApplyAppearance = apply;
  if (dark && dark.addEventListener) dark.addEventListener('change', apply);
  apply();
})();
