
// This is synchronized with ColorThemeStorage class

function initializeColorTheme() {
  let v = window.localStorage.getItem('thebridsk:bridge:color-theme')
  if (typeof(v) == 'string') {
    document.body.setAttribute('data-theme',v)
  }
}

initializeColorTheme()
