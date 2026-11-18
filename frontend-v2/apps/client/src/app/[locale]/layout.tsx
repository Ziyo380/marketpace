import '../global.css';
import '../marketplace.css';
import type { Metadata } from 'next';
import { notFound } from 'next/navigation';
import { hasLocale, NextIntlClientProvider } from 'next-intl';
import { routing } from '../../i18n/routing';

type Props = {
  children: React.ReactNode;
  params: Promise<{ locale: string }>;
};

export function generateStaticParams() {
  return routing.locales.map((locale) => ({ locale }));
}

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { locale } = await params;
  if (!hasLocale(routing.locales, locale)) notFound();
  return { title: 'Makon Marketplace', description: 'Современный маркетплейс для покупателей и локального бизнеса Узбекистана.' };
}

export default async function LocaleLayout({ children, params }: Props) {
  const { locale } = await params;
  if (!hasLocale(routing.locales, locale)) notFound();

  return (
    <html lang={locale}>
      <body><NextIntlClientProvider>{children}</NextIntlClientProvider></body>
    </html>
  );
}
