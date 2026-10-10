import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import zhTw from 'element-plus/dist/locale/zh-tw.mjs'
import en from 'element-plus/dist/locale/en.mjs'
import { languageOptions, translations } from '@/i18n'

const elementPlusLocales = {
  'en': en,
  'zh-TW': zhTw,
  'zh-CN': zhCn
}

function getPreferredLocale() {
  const stored = localStorage.getItem('scf-locale')
  if (stored && elementPlusLocales[stored]) return stored

  const browserLang = navigator.language || 'zh-CN'
  if (browserLang.toLowerCase().startsWith('zh-tw')) return 'zh-TW'
  if (browserLang.toLowerCase().startsWith('zh')) return 'zh-CN'
  return 'en'
}

function getValueByPath(obj, path) {
  return path.split('.').reduce((current, key) => (current && current[key] !== undefined ? current[key] : undefined), obj)
}

export const useLocaleStore = defineStore('locale', () => {
  const locale = ref(getPreferredLocale())

  const currentLocale = computed(() => elementPlusLocales[locale.value] || zhCn)

  function setLocale(nextLocale) {
    const normalized = elementPlusLocales[nextLocale] ? nextLocale : 'zh-CN'
    locale.value = normalized
    localStorage.setItem('scf-locale', normalized)
    document.documentElement.lang = normalized
  }

  function t(key, fallback = '') {
    const selected = translations[locale.value] || translations['zh-CN']
    const value = getValueByPath(selected, key)
    if (value !== undefined && value !== null) return String(value)

    const english = translations.en
    const fallbackValue = getValueByPath(english, key)
    if (fallbackValue !== undefined && fallbackValue !== null) return String(fallbackValue)

    return fallback || key
  }

  function getLanguageLabel(value = locale.value) {
    return languageOptions.find(option => option.value === value)?.label || '简体中文'
  }

  setLocale(locale.value)

  return {
    locale,
    currentLocale,
    setLocale,
    t,
    getLanguageLabel
  }
})
