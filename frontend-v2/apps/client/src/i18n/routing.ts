import { defineRouting } from 'next-intl/routing';

export const routing = defineRouting({
  locales: ['ru'],
  defaultLocale: 'ru',
  localePrefix: 'always',
  localeDetection: true,
});

export type AppLocale = (typeof routing.locales)[number];
